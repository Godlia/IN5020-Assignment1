library(tidyverse)


results_dir <- "/Users/kine/Desktop/IN5020/results"
graphs_dir <- "/Users/kine/Desktop/IN5020/graphs"


# Lager directory hvis den ikke eksisterer allerede
dir.create(
  graphs_dir,
  showWarnings = FALSE
)


files <- list.files(
  path = results_dir,
  pattern = "^(naive_server|server_cache|client_cache)_T(20|50)\\.txt$",
  full.names = TRUE
)


# Stop immediately if no files were found
if (length(files) == 0) {
  stop("No result files found in: ", results_dir)
}


cat("Found", length(files), "result files:\n")

for (file in files) {
  cat(" -", basename(file), "\n")
}

# Lager datarammer

all_results <- tibble()

# Prosesserer hver fil 

for (file in files) {
  
  filename <- basename(file)
  
  cat("\nProcessing:", filename, "\n")
  

# Finner implementasjon fra navnet (naive/server etc)
  
  if (str_detect(filename, "^naive_server")) {
    
    implementation <- "Naive"
    
  } else if (str_detect(filename, "^server_cache")) {
    
    implementation <- "Server cache"
    
  } else if (str_detect(filename, "^client_cache")) {
    
    implementation <- "Client cache"
    
  } else {
    
    stop("Unknown file type: ", filename)
  }
  
# Finner T fra filnavn (20 vs 50)
  
  T_value <- str_match(
    filename,
    "_T([0-9]+)\\.txt$"
  )[, 2]
  
  T_value <- as.integer(T_value)
  
  
  lines <- read_lines(file)
  

  
  query_lines <- lines[
    str_detect(lines, "turnaround time:") &
      str_detect(lines, "processed by Server")
  ]
  
  # Sjekker at det eksisterer queries
  if (length(query_lines) == 0) {
    
    stop(
      "No query-result lines found in ",
      filename
    )
  }
  
# Henter ut turnaround time
  
  turnaround_ms <- str_match(
    query_lines,
    "turnaround time:\\s*([0-9.]+)\\s*ms"
  )[, 2]
  
  turnaround_ms <- as.numeric(turnaround_ms)
  
  
  if (any(is.na(turnaround_ms))) {
    
    stop(
      "Could not extract all turnaround times from ",
      filename
    )
  }
  
  # Lager datarammer
  
  file_results <- tibble(
    
    query_number = seq_along(turnaround_ms),
    
    turnaround_ms = turnaround_ms,
    
    implementation = implementation,
    
    T = T_value,
    
    source_file = filename
  )
  
  
  # Show what we extracted
  print(file_results)
  

  
  all_results <- bind_rows(
    all_results,
    file_results
  )
  
# Lager individuelle grafer
  
  plot_title <- paste0(
    implementation,
    " — T = ",
    T_value,
    " ms"
  )
  
  
  p <- ggplot(
    file_results,
    aes(
      x = query_number,
      y = turnaround_ms
    )
  ) +
    
    geom_line() +
    
    labs(
      title = plot_title,
      x = "Query number",
      y = "Turnaround time (ms)"
    ) +
    
    theme_minimal()
  
  
  # Show graph when running interactively in RStudio
  print(p)
  
# Filnavn for output
  
  graph_filename <- filename |>
    str_remove("\\.txt$") |>
    paste0(".png")
  
  
  graph_path <- file.path(
    graphs_dir,
    graph_filename
  )
  
  
# Lagrer grafer
  
  ggsave(
    filename = graph_path,
    plot = p,
    width = 10,
    height = 6,
    dpi = 300
  )
  
  
  cat("Saved:", graph_path, "\n")
}


# Lager grafer for sammenligning hvis det er nødvendig (tror det er optional)

for (T_value in sort(unique(all_results$T))) {
  
  
  comparison_data <- all_results |>
    filter(T == T_value)
  
  
  p_comparison <- ggplot(
    comparison_data,
    aes(
      x = query_number,
      y = turnaround_ms,
      group = implementation,
      linetype = implementation
    )
  ) +
    
    geom_line() +
    
    labs(
      title = paste0(
        "Turnaround time comparison — T = ",
        T_value,
        " ms"
      ),
      x = "Query number",
      y = "Turnaround time (ms)",
      linetype = "Implementation"
    ) +
    
    theme_minimal()
  
  
  print(p_comparison)
  
  
  comparison_path <- file.path(
    graphs_dir,
    paste0(
      "comparison_T",
      T_value,
      ".png"
    )
  )
  
  
  ggsave(
    filename = comparison_path,
    plot = p_comparison,
    width = 11,
    height = 7,
    dpi = 300
  )
  
  
  cat("Saved:", comparison_path, "\n")
}