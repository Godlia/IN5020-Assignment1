library(tidyverse)

results_dir <- "/Users/kine/Desktop/IN5020/results"
graphs_dir <- "/Users/kine/Desktop/IN5020/graphs"

# lager directory om den ikke eksisterer

dir.create(
  graphs_dir,
  recursive = TRUE,
  showWarnings = FALSE
)

# finner logfilene

files <- list.files(
  path = results_dir,
  pattern = "_server[0-9]+_queue\\.txt$",
  full.names = TRUE
)


if (length(files) == 0) {
  stop("No server queue log files found in: ", results_dir)
}


cat("Found", length(files), "queue log files:\n")

for (file in files) {
  cat(" -", basename(file), "\n")
}

all_queue_data <- tibble()


for (file in files) {
  
  filename <- basename(file)
  
  cat("\nProcessing:", filename, "\n")
  
# finner implementasjonsmetode fra filnavnet
  
  if (str_detect(filename, "^naive_server")) {
    
    implementation <- "Naive"
    
  } else if (str_detect(filename, "^server_cache")) {
    
    implementation <- "Server cache"
    
  } else if (str_detect(filename, "^client_cache")) {
    
    implementation <- "Client cache"
    
  } else {
    
    stop("Unknown implementation in filename: ", filename)
  }
  
  
  # Henter hvilken T-verdi det er
  
  T_value <- str_match(
    filename,
    "_T([0-9]+)_"
  )[, 2]
  
  T_value <- as.integer(T_value)
  
  
# Henter ut servernummer
  
  server_number <- str_match(
    filename,
    "_server([0-9]+)_queue\\.txt$"
  )[, 2]
  
  server_number <- as.integer(server_number)
  

  
  queue_data <- read_table(
    file,
    col_types = cols(
      timestamp = col_double(),
      queue_size = col_integer()
    )
  )
  

  
  if (!all(c("timestamp", "queue_size") %in% names(queue_data))) {
    
    stop(
      "Queue log does not contain timestamp and queue_size: ",
      filename
    )
  }

  
  queue_data <- queue_data |>
    mutate(
      server = server_number,
      implementation = implementation,
      T = T_value,
      source_file = filename
    )
  
  
  print(queue_data)
  

  all_queue_data <- bind_rows(
    all_queue_data,
    queue_data
  )
  
# Lager grafer
  
  p <- ggplot(
    queue_data,
    aes(
      x = timestamp,
      y = queue_size
    )
  ) +
    
    geom_step() +
    
    labs(
      title = paste0(
        implementation,
        " — Server ",
        server_number,
        " — T = ",
        T_value,
        " ms"
      ),
      x = "Unix timestamp",
      y = "Queue size"
    ) +
    
    theme_minimal()
  
  
  print(p)
  
# Lagrer graf
  
  graph_filename <- filename |>
    str_remove("\\.txt$") |>
    paste0(".png")
  
  
  graph_path <- file.path(
    graphs_dir,
    graph_filename
  )
  
  
  ggsave(
    filename = graph_path,
    plot = p,
    width = 10,
    height = 6,
    dpi = 300
  )
  
  
  cat("Saved:", graph_path, "\n")
}


# Lagrer graf for alle servere kombinert

runs <- all_queue_data |>
  distinct(
    implementation,
    T
  )


for (i in seq_len(nrow(runs))) {
  
  current_implementation <- runs$implementation[i]
  current_T <- runs$T[i]
  
  
  run_data <- all_queue_data |>
    filter(
      implementation == current_implementation,
      T == current_T
    )
  
  
  p_combined <- ggplot(
    run_data,
    aes(
      x = timestamp,
      y = queue_size
    )
  ) +
    
    geom_step() +
    
    facet_wrap(
      ~ server,
      scales = "free_x",
      labeller = labeller(
        server = function(x) paste("Server", x)
      )
    ) +
    
    labs(
      title = paste0(
        current_implementation,
        " — Server queues — T = ",
        current_T,
        " ms"
      ),
      x = "Unix timestamp",
      y = "Queue size"
    ) +
    
    theme_minimal()
  
  
  print(p_combined)
  
  implementation_filename <- current_implementation |>
    str_to_lower() |>
    str_replace_all(" ", "_")
  
  
  output_path <- file.path(
    graphs_dir,
    paste0(
      implementation_filename,
      "_T",
      current_T,
      "_all_servers.png"
    )
  )
  
  
  ggsave(
    filename = output_path,
    plot = p_combined,
    width = 14,
    height = 8,
    dpi = 300
  )

}