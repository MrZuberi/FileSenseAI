# FileSenseAI: An AI-Powered File Organizer

FileSenseAI reads every file in a folder you choose, understands what each one is actually about using AI embeddings, groups them into topic clusters, has an AI name each topic, and reorganizes the folder so you end up with zero loose files, just a clean set of sensibly named topic folders. It runs as a local web app, opening automatically in your browser.

## Key Features

- **Understands File Content, Not Just File Names**: Uses Apache Tika to extract real text from PDFs, Word documents, and plain text files, then Cohere's embedding model to understand what each file is actually about.
- **Automatic Topic Clustering**: Groups files by genuine semantic similarity using K-means clustering, not by file extension or naming pattern.
- **AI-Named Folders, With a Smart Fallback**: An LLM names each cluster based on its real content. If that call ever fails, the fallback name is built from the cluster's actual file names instead of a generic bucket.
- **Fast**: File reading and folder naming both run in parallel across multiple threads.
- **Safe File Moves**: Automatically renames on collision rather than overwriting if two files would land in the same folder with the same name.

## Technologies Used

- **Java 17**
- **Spring Boot**: Runs the whole application, including the embedded web server.
- **Apache Tika**: Extracts real text content from PDFs, Word documents, and other file formats.
- **LangChain4j**: Provides the document splitting utility used to extract representative excerpts from each file before embedding.
- **Cohere**: The embedding model that turns file content into a vector, and the language model that names each topic cluster.
- **Smile**: Java's machine learning library, used for K-means clustering of the embedding vectors.
- **HTML, CSS, and JavaScript**: The local web UI, served directly by Spring Boot with no separate build step.
- **Maven**: Build tool.

## How Files Get Sorted

1. You pick a folder in the browser UI.
2. Every file in that folder gets its text extracted with Apache Tika, in parallel across multiple threads.
3. Each file's text gets embedded by Cohere into a vector, built from excerpts taken from the start, middle, and end of the file for a more representative signal.
4. Smile's K-means clustering groups those vectors into topic clusters.
5. For each cluster, Cohere is asked to suggest a short folder name based on real file names and content samples, run in parallel across several clusters at once.
6. The tool creates those folders and moves every file into its match.
7. You're left with zero loose files, and a clear "Done" screen showing exactly what went where.

## Prerequisites

- JDK 17
- Maven 3.9 or later
- A free Cohere API key from `https://dashboard.cohere.com/api-keys`

## Installation

**Clone the repository:**

**Build:**

**Run:**

A browser tab opens automatically at `http://localhost:8080`.

## Using It

1. The browser UI opens showing your home folder, click into subfolders to navigate.
2. Once you're in the folder you want organized, click "Organize This Folder."
3. Choose whether to back up to S3 first, if configured.
4. Watch the live progress bar and status line as it reads, embeds, clusters, names, and moves your files.
5. A "Done" screen shows every folder created and how many files landed in each.

## A Note on Scope

FileSenseAI organizes one folder you point it at, it does not run in the background watching your whole file system. That's a deliberate, focused choice.

## How It Works

This section walks through the codebase in the order it was actually built.

1. [`pom.xml`](pom.xml) declares every dependency, Spring Boot's web starter, Tika, LangChain4j, Smile, and the AWS SDK.
2. [`src/main/java/com/filesenseai/FileSenseAiApplication.java`](src/main/java/com/filesenseai/FileSenseAiApplication.java) is the entry point, starting Spring Boot's embedded web server.
3. [`src/main/java/com/filesenseai/config/AppProperties.java`](src/main/java/com/filesenseai/config/AppProperties.java) reads the Cohere API key and optional AWS settings from environment variables.
4. [`src/main/java/com/filesenseai/ai/CohereClient.java`](src/main/java/com/filesenseai/ai/CohereClient.java) is the only file that talks to Cohere directly, embedding batches of text and naming clusters, with real errors instead of silent fallbacks.
5. [`src/main/java/com/filesenseai/backup/S3BackupService.java`](src/main/java/com/filesenseai/backup/S3BackupService.java) zips a folder and uploads it to S3.
6. [`src/main/java/com/filesenseai/extraction/TextExtractionService.java`](src/main/java/com/filesenseai/extraction/TextExtractionService.java) extracts text from every file in a folder in parallel, reporting progress per file.
7. [`src/main/java/com/filesenseai/embedding/EmbeddingService.java`](src/main/java/com/filesenseai/embedding/EmbeddingService.java) uses LangChain4j's document splitter to build a start-middle-end excerpt from each file, then embeds those with Cohere.
8. [`src/main/java/com/filesenseai/clustering/ClusteringService.java`](src/main/java/com/filesenseai/clustering/ClusteringService.java) runs Smile's K-means algorithm on those vectors to group files into topic clusters.
9. [`src/main/java/com/filesenseai/clustering/ClusterNamer.java`](src/main/java/com/filesenseai/clustering/ClusterNamer.java) names each cluster in parallel, falling back to a name built from real file names if the AI call fails.
10. [`src/main/java/com/filesenseai/organize/FileOrganizer.java`](src/main/java/com/filesenseai/organize/FileOrganizer.java) builds a plan mapping folder names to files, then creates the folders and moves the files, handling name collisions safely.
11. [`src/main/java/com/filesenseai/pipeline/SortingPipeline.java`](src/main/java/com/filesenseai/pipeline/SortingPipeline.java) strings every step together in order, reporting fine-grained progress throughout.
12. [`src/main/java/com/filesenseai/job/JobManager.java`](src/main/java/com/filesenseai/job/JobManager.java) runs the pipeline in the background as a trackable job, so the web UI can poll its status without blocking.
13. [`src/main/java/com/filesenseai/web/SortController.java`](src/main/java/com/filesenseai/web/SortController.java) exposes the REST API the frontend calls, folder browsing, starting a job, and checking job status.
14. [`src/main/java/com/filesenseai/web/BrowserLauncher.java`](src/main/java/com/filesenseai/web/BrowserLauncher.java) automatically opens your default browser to the app once the server is ready.
15. [`src/main/resources/static/index.html`](src/main/resources/static/index.html) is the entire frontend, the folder browser, the live progress bar, and the results screen.

## License

MIT License.
