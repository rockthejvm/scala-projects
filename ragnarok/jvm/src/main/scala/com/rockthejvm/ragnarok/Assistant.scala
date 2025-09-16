package com.rockthejvm.ragnarok

import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.document.source.FileSystemSource
import dev.langchain4j.data.document.{Document, DocumentLoader}
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.{OpenAiChatModel, OpenAiStreamingChatModel}
import dev.langchain4j.rag.{DefaultRetrievalAugmentor, RetrievalAugmentor}
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.service.{AiServices, SystemMessage, TokenStream, UserMessage}
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore

import scala.jdk.CollectionConverters.*

trait Assistant {
  @SystemMessage(Array("You are an agent designed to answer queries over a set of given programming articles. Do not answer any query unrelated to programming or software engineering. Always use the tools provided to answer a question. Prioritize information from the articles and include code snippets where necessary."))
  def reply(@UserMessage question: String): TokenStream
}

object RAGEngine {
  val apiKey = sys.env.getOrElse("OPENAI_API_KEY", "REPLACE_ME")
  val contentPath = os.Path("/Users/daniel/dev/rockthejvm/courses/scala-projects/ragnarok/jvm/src/main/resources/blog")
  val model = "gpt-4o"

  val chatModel = OpenAiStreamingChatModel.builder()
    .apiKey(apiKey)
    .modelName(model)
    .temperature(0.2) // more deterministic responses
    .build()

  // documents
  val parser = new TextDocumentParser()
  val documents: List[Document] =
    os.list(contentPath)
      .toList
      .filter(os.isFile)
      .map { p =>
        DocumentLoader.load(FileSystemSource.from(p.toNIO), parser)
      }

  // split the docs
  val splitter = DocumentSplitters.recursive(200, 50)

  // ingest the splits => embedding store
  val store = new InMemoryEmbeddingStore[TextSegment]()
  val ingestor = EmbeddingStoreIngestor.builder()
    .documentSplitter(splitter)
    .embeddingStore(store)
    .build()

  def startIngestion(): Unit =
    documents.foreach(ingestor.ingest)

  // retriever
  val retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .maxResults(3) // number of references per query
    .build()

  // augmentor
  val augmentor: RetrievalAugmentor = DefaultRetrievalAugmentor.builder()
    .contentRetriever(retriever)
    .build()

  def apply() = AiServices.builder(classOf[Assistant])
    .streamingChatModel(chatModel)
    .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
    .retrievalAugmentor(augmentor)
    .build()

  def getReferences(question: String) = {
    val relevantContent = retriever.retrieve(Query.from(question)).asScala

    relevantContent.flatMap { content =>
      Option(
        content
          .textSegment()
          .metadata()
          .getString("file_name")
      )
    }
      .filterNot(_.isBlank)
      .distinct // evaluation-modes-in-scala.mdx
      .map(_.split("\\.")(0))
  }
}