package com.rockthejvm.librarydemos

import upickle.default.*

object UPickleDemo {
  def main(args: Array[String]): Unit = {
    // fundamental methods: read and write
    val jsonNumber = write(42)
    println(s"single number as json: $jsonNumber")
    val stringJson = write("Scala")
    println(s"single string as json: $stringJson")
    val number = read[Int](jsonNumber)
    println(number + 1)

    // collections
    val arrayJson = write(List(1,2,3)) // [1,2,3]
    println(s"array as json: $arrayJson")

    // maps
    val mapJson = write(Map("name" -> "Daniel", "website" -> "Rock the JVM"))
    println(s"map to json: $mapJson")

    // case classes
    case class Person(name: String, age: Int) derives ReadWriter

    val person = Person("John", 30)
    val personJson = write(person)
    println(s"person to json: $personJson")
    val personAgain = read[Person](personJson)
    println(s"same person? ${personAgain == person}")

    // nesting
    case class Address(street: String, city: String) derives ReadWriter
    case class Employee(name: String, addresses: List[Address]) derives ReadWriter

    val employee = Employee("Alice", List(Address("123 Main Str", "Gotham"), Address("987 Big St", "Gotham")))
    val employeeJson = write(employee)
    println(s"employee to json: $employeeJson")

    // custom fields - best for keeping/maintaining compatibility with existing json values elsewhere
    case class User(
                     @upickle.implicits.key("user_id") id: Int,
                     @upickle.implicits.key("user_name") name: String
                   ) derives ReadWriter

    val user = User(1, "Bob")
    val userJson = write(user)
    println(s"user as json: $userJson")

    // sealed traits
    sealed trait Pet derives ReadWriter // possible as long as ALL subtypes derive ReadWriter
    case class Dog(name: String, breed: String) extends Pet derives ReadWriter
    case class Puppy(name: String, breed: String) extends Pet derives ReadWriter
    case class Cat(name: String, lives: Int) extends Pet derives ReadWriter

    val pets: List[Pet] = List(Dog("Rex", "German Shepherd"), Cat("Garfield", 9))
    val petsJson = write(pets)
    println(s"pets as json: $petsJson")
    val petsBack = read[List[Pet]](petsJson)
    println(s"pets back: $petsBack")

    // read partial data from json
    val complexJson =
      """
        |{
        |   "name": "Dave",
        |   "age": 45,
        |   "address": { "city": "Bucharest" },
        |   "hobbies": ["guitar", "programming"]
        |}
        |""".stripMargin

    val jsonAST = read[ujson.Value](complexJson)
    val name = jsonAST("name").str // can only call .str on String values!
    val address = jsonAST("address")
    val city = address("city").str
    val city_v2 = jsonAST("address")("city").str
    val firstHobby = jsonAST("hobbies")(0).str
    println(name)
    println(city)
    println(firstHobby)
  }
}
