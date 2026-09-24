$ErrorActionPreference = 'Stop'

$ScriptDir = $PSScriptRoot
Set-Location $ScriptDir

$backup = $args.Count -eq 1 -and $args[0] -eq '--backup'
if ($args.Count -gt 1 -or ($args.Count -eq 1 -and -not $backup)) {
	Write-Error "Usage: .\runwin.ps1 [--backup]"
	exit 2
}

if ($backup) {
	$cacheTypes = @('naive-server', 'cache-server', 'client-cache')
} else {
	$cacheTypes = @('FIFO', 'LRU', 'NAIVE')
}
$clientDelaysMs = @(50, 20)
$maxParallel = 3
$projects = New-Object System.Collections.Generic.List[string]
$batchJobs = New-Object System.Collections.Generic.List[object]
$exitStatus = 0

# Docker/Maven write progress to stderr; print it as plain text and only fail on the exit code
function Invoke-Native {
	param([scriptblock]$Command)
	$ErrorActionPreference = 'Continue'
	& $Command 2>&1 | ForEach-Object { "$_" }
	if ($LASTEXITCODE -ne 0) {
		throw "Command failed with exit code ${LASTEXITCODE}: $Command"
	}
}

function Wait-Batch {
	foreach ($job in $batchJobs) {
		Wait-Job $job | Out-Null
		Receive-Job $job -ErrorAction Continue
		if ($job.State -ne 'Completed') {
			$script:exitStatus = 1
		}
		Remove-Job $job
	}
	$batchJobs.Clear()
}

$runScenario = {
	param($ScriptDir, $CacheType, $ClientDelayMs, $RunName, $Project, $ProxyPort, $Backup)

	$ErrorActionPreference = 'Stop'
	Set-Location $ScriptDir

	function Invoke-Native {
		param([scriptblock]$Command)
		$ErrorActionPreference = 'Continue'
		& $Command 2>&1 | ForEach-Object { "$_" }
		if ($LASTEXITCODE -ne 0) {
			throw "Command failed with exit code ${LASTEXITCODE}: $Command"
		}
	}

	if ($Backup) {
		$outputFilename = "$CacheType.txt"
		switch ($CacheType) {
			'naive-server' {
				$serverCacheType = 'NAIVE'
				$clientCacheMode = 'NAIVE'
			}
			'cache-server' {
				$serverCacheType = 'FIFO'
				$clientCacheMode = 'NAIVE'
			}
			'client-cache' {
				$serverCacheType = 'NAIVE'
				$clientCacheMode = 'FIFO'
			}
		}
	} else {
		$outputFilename = 'client-output.txt'
		$serverCacheType = $CacheType
		$clientCacheMode = $CacheType
	}

	if ($serverCacheType -eq 'NAIVE') {
		$serverCacheMode = 'NAIVE'
		$cachePolicy = 'LRU'
	} else {
		$serverCacheMode = 'SERVER'
		$cachePolicy = $serverCacheType
	}

	New-Item -ItemType Directory -Force "output/$RunName" | Out-Null
	Write-Output "Running cache=$CacheType delay=${ClientDelayMs}ms (project=$Project)"

	$env:CACHE_TYPE = $CacheType
	$env:SERVER_CACHE_TYPE = $serverCacheType
	$env:CLIENT_CACHE_MODE = $clientCacheMode
	$env:CLIENT_DELAY_MS = "$ClientDelayMs"
	$env:RUN_NAME = $RunName
	$env:OUTPUT_FILENAME = $outputFilename
	$env:SERVER_CACHE_MODE = $serverCacheMode
	$env:CACHE_POLICY = $cachePolicy
	$env:PROXY_PORT = "$ProxyPort"

	Invoke-Native { docker compose -p $Project up -d }
	Invoke-Native { docker compose -p $Project wait client }
	Invoke-Native { docker compose -p $Project down --remove-orphans }
}

try {
	Write-Output "Building project..."
	Invoke-Native { .\mvnw.cmd clean compile package }

	Write-Output "Stopping any previous run..."
	Invoke-Native { docker compose down --remove-orphans }

	Invoke-Native { docker compose build --no-cache }

	foreach ($cacheType in $cacheTypes) {
		foreach ($clientDelayMs in $clientDelaysMs) {
			$runName = "$cacheType$clientDelayMs"
			$project = "in5020-$($runName.ToLower())"
			$proxyPort = 1100 + $projects.Count
			$projects.Add($project)

			$job = Start-Job -ScriptBlock $runScenario -ArgumentList $ScriptDir, $cacheType, $clientDelayMs, $runName, $project, $proxyPort, $backup
			$batchJobs.Add($job)

			if ($batchJobs.Count -eq $maxParallel) {
				Wait-Batch
			}
		}
	}

	if ($batchJobs.Count -gt 0) {
		Wait-Batch
	}
} catch {
	Write-Error $_ -ErrorAction Continue
	$exitStatus = 1
} finally {
	$ErrorActionPreference = 'Continue'
	Get-Job | Where-Object { $batchJobs -contains $_ } | Stop-Job -PassThru | Remove-Job -Force
	foreach ($project in $projects) {
		docker compose -p $project down --remove-orphans *> $null
	}
}

exit $exitStatus
