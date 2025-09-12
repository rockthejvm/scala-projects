package com.rockthejvm.librarydemos

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.message.UserMessage as Message
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.response.{ChatResponse, StreamingChatResponseHandler}
import dev.langchain4j.model.openai.{OpenAiChatModel, OpenAiStreamingChatModel}
import dev.langchain4j.rag.{DefaultRetrievalAugmentor, RetrievalAugmentor}
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.{AiServices, SystemMessage, UserMessage}
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore

object LangChain4jDemo {
  // OpenAI API - costs $$
  // platform.openai.com - generate an API key!
  val apiKey = sys.env.getOrElse("OPENAI_API_KEY", "REPLACE_ME")
  val model = "gpt-4o"

  val chatModel = OpenAiChatModel.builder()
    .apiKey(apiKey)
    .modelName(model)
    .temperature(0.2) // more deterministic responses
    .build()

  def demoBasicResponse(): Unit = {
    // basic api: call "chat"
    val basicResponse = chatModel.chat("Generate interesting facts about 5 imaginary people. Write 2 paragraphs about each person.")
    println(s"Basic response: $basicResponse")
  }

  // memory
  def demoMemory(): Unit = {
    val memory: ChatMemory = MessageWindowChatMemory.withMaxMessages(20)

    def chatWithMemory(message: String): String = {
      // memorize my request
      memory.add(new Message(message))
      // send the request
      val response = chatModel.chat(memory.messages())
      val stringRep = response.toString
      // memorize the bot's response
      memory.add(response.aiMessage())
      stringRep
    }

    println(chatWithMemory("Please remember my favorite color is blue."))
    println(chatWithMemory("What is my favorite color? Be brief."))
  }

  // assistant API
  trait SupportAssistant {
    @SystemMessage(Array("You are a concise, helpful assistant."))
    def reply(@UserMessage question: String): String
  }

  def demoAssistant(): Unit = {
    val assistant = AiServices.builder(classOf[SupportAssistant])
      .chatModel(chatModel)
      .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
      .build()

    val response1 = assistant.reply("Tell me about the Scala language in 2 sentences.")
    val response2 = assistant.reply("Now expand this description with one more fact.")

    println(response1)
    println(response2)
  }

  // streaming
  def demoStreaming(): Unit = {
    val streamingModel =
      OpenAiStreamingChatModel.builder()
        .apiKey(apiKey)
        .modelName(model)
        .temperature(0.2)
        .build()

    streamingModel.chat("Tell me one paragraph about Scala.", new StreamingChatResponseHandler {
      override def onPartialResponse(partialResponse: String): Unit =
        println(partialResponse)

      override def onCompleteResponse(completeResponse: ChatResponse): Unit =
        println(completeResponse.toString)

      override def onError(error: Throwable): Unit =
        error.printStackTrace()
    })

    Thread.sleep(10000)
  }

  // RAG
  def demoRAG(): Unit = {
    // documents
    val documents =
      List(
        """
          |Elara Voss is a renowned astro-botanist who has dedicated her life to studying plant life in extraterrestrial environments. Born in the small town of Larkspur, she grew up surrounded by lush forests, which sparked her fascination with plant biology. Her groundbreaking research on the adaptability of Earth plants to Martian soil has earned her numerous accolades, including the prestigious Celestial Science Award. Elara's work has been instrumental in the development of sustainable agriculture systems for future Mars colonies. Her innovative approach combines genetic engineering with traditional botany, allowing plants to thrive in the harsh conditions of space.
          |Beyond her scientific achievements, Elara is also an accomplished artist. She uses her art to communicate complex scientific concepts to the public, creating stunning visual representations of her research. Her art exhibits, which blend science and creativity, have toured globally, inspiring countless young scientists and artists alike. Elara's unique ability to bridge the gap between science and art has made her a beloved figure in both communities. She often speaks at international conferences, advocating for interdisciplinary collaboration and the importance of creativity in scientific innovation.
          |""".stripMargin,
        """
          |Kael Thorne is a celebrated chef and culinary historian known for his innovative fusion of ancient recipes with modern techniques. Raised in a multicultural household in New Orleans, Kael was exposed to a diverse array of flavors and cooking styles from a young age. His passion for culinary history led him to travel extensively, studying under master chefs and historians around the world. Kael's restaurant, "Epoch," is famous for its ever-changing menu that features dishes inspired by historical periods, from the Roman Empire to the Ming Dynasty. His meticulous research and attention to detail have earned him a Michelin star and a devoted following of food enthusiasts.
          |In addition to his culinary pursuits, Kael is an avid storyteller. He hosts a popular podcast, "Feasts of the Past," where he delves into the history and cultural significance of various dishes. Each episode is a journey through time, exploring how food has shaped societies and traditions. Kael's engaging storytelling and deep knowledge have made the podcast a hit, attracting listeners from all over the world. He is also a passionate advocate for sustainable food practices, often incorporating locally sourced and seasonal ingredients into his creations. Through his work, Kael aims to preserve culinary heritage while inspiring a new generation of chefs to innovate and explore.
          |""".stripMargin,
        """
          |Zara Nyx is a pioneering technologist and entrepreneur who has revolutionized the field of virtual reality (VR). Growing up in a tech-savvy family in Silicon Valley, Zara was always fascinated by the potential of technology to transform human experiences. She founded her company, "Dreamscape Innovations," with the vision of creating immersive VR environments that enhance learning and creativity. Her flagship product, "Mindscape," is an educational VR platform that allows users to explore complex subjects, from quantum physics to ancient history, in an interactive and engaging way. Zara's work has been praised for its ability to make learning accessible and enjoyable for people of all ages.
          |Outside of her tech endeavors, Zara is a passionate advocate for diversity and inclusion in the tech industry. She has established several initiatives aimed at supporting underrepresented groups in technology, providing mentorship and resources to aspiring technologists. Zara frequently speaks at conferences and panels, sharing her insights on the importance of diverse perspectives in driving innovation. Her commitment to fostering an inclusive tech community has earned her recognition as one of the most influential women in technology. Zara's vision for the future is one where technology serves as a tool for empowerment and positive change.
          |""".stripMargin,
        """
          |Orion Gale is a celebrated environmental activist and documentary filmmaker whose work has brought global attention to the plight of endangered ecosystems. Born in the coastal town of Clearwater, Orion developed a deep connection to the ocean and its inhabitants from an early age. His passion for environmental conservation led him to pursue a career in filmmaking, using the power of visual storytelling to raise awareness about critical environmental issues. Orion's documentaries, such as "Whispers of the Wild" and "The Last Sanctuary," have been acclaimed for their breathtaking cinematography and compelling narratives, earning him multiple awards and a dedicated following.
          |In addition to his filmmaking, Orion is an active participant in various conservation initiatives. He collaborates with environmental organizations to develop strategies for protecting vulnerable habitats and species. Orion's commitment to conservation extends beyond his professional work; he is known for organizing community clean-up events and educational workshops to inspire local action. His efforts have not only highlighted the urgent need for environmental protection but have also empowered individuals to make a difference in their communities. Orion's unwavering dedication to preserving the natural world continues to inspire and mobilize people around the globe.
          |""".stripMargin,
        """
        |Lyra Solis is a visionary architect and urban planner renowned for her innovative designs that prioritize sustainability and community well-being. Raised in the bustling city of Metropolis, Lyra witnessed firsthand the challenges of urban living, which fueled her desire to create more livable and environmentally friendly spaces. Her firm, "EcoSphere Designs," is at the forefront of sustainable architecture, known for its use of eco-friendly materials and integration of green spaces into urban environments. Lyra's projects, such as the award-winning "Green Haven" residential complex, have set new standards for sustainable urban living, earning her international acclaim.
        |Beyond her architectural achievements, Lyra is a passionate advocate for social equity in urban development. She believes that everyone deserves access to safe, healthy, and beautiful living spaces, regardless of their socioeconomic status. Lyra actively collaborates with community organizations to ensure that her projects address the needs of diverse populations. Her commitment to inclusive design has made her a respected leader in the field of urban planning. Lyra frequently lectures at universities and conferences, sharing her insights on the intersection of sustainability, design, and social justice. Her work continues to inspire a new generation of architects to create spaces that are not only functional but also equitable and harmonious with nature.
        |""".stripMargin
      )

    // split the docs
    val splitter = DocumentSplitters.recursive(200, 50)

    // ingest the splits => embedding store
    val store = new InMemoryEmbeddingStore[TextSegment]()
    val ingestor = EmbeddingStoreIngestor.builder()
      .documentSplitter(splitter)
      .embeddingStore(store)
      .build()

    documents.map(Document.from).foreach(ingestor.ingest)

    // retriever
    val retriever = EmbeddingStoreContentRetriever.builder()
      .embeddingStore(store)
      .maxResults(3) // number of references per query
      .build()

    // augmentor
    val augmentor: RetrievalAugmentor = DefaultRetrievalAugmentor.builder()
      .contentRetriever(retriever)
      .build()

    // assistant
    trait DocAssistant {
      @SystemMessage(Array("Answer using retrieved data. If uncertain, say you don't know."))
      def reply(@UserMessage question: String): String
    }

    val docAssistant = AiServices.builder(classOf[DocAssistant])
      .chatModel(chatModel)
      .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
      .retrievalAugmentor(augmentor)
      .build()

    val answer = docAssistant.reply("What is the main job of Elara Voss?")
    println(s"RAG reply: $answer")
  }

  def main(args: Array[String]): Unit = {
    demoRAG()
  }
}
