package com.rockthejvm.librarydemos

import scalasql.*
import scalasql.simple.{*, given}
import scalasql.dialects.PostgresDialect.*

import java.sql.DriverManager

case class Player(id: String, name: String, email: String, xp: Int, gameType: String)
object Player extends SimpleTable[Player]

case class Game(id: String, name: String, gameType: String)
object Game extends SimpleTable[Game]

object ScalaSqlDemo {
  def main(args: Array[String]): Unit = {
    // database
    val dbClient = new DbClient.Connection(
      DriverManager.getConnection("jdbc:postgresql://localhost:5432/", "docker", "docker"),
      // can configure name conversion e.g. if you have another casing other than snake_case-CamelCase
      new Config {
        override def nameMapper(v: String) = super.nameMapper(v)
      }
    )
    val db = dbClient.getAutoCommitClientConnection

    // queries
    val basicQuery = Player.select // select * from player
    println(db.renderSql(basicQuery))
    val result = db.run(basicQuery) // Vector[Player]
    println(result)

    // filters
    val findByIdQuery = Player.select.filter(_.id === "p3") // select * from player where id = 'p3'
    //                                       ^ not a Player, === used to build Expr, NOT the == for JVM objects
    println(db.renderSql(findByIdQuery))
    println(db.run(findByIdQuery))

    // math operators are okay
    val highXpQuery = Player.select.filter(_.xp > 2000)

    // mapping of results
    val namesQuery = Player.select.map(_.name) // select name from player
    println(db.run(namesQuery))

    // aggregates
    val highXpCount = highXpQuery.size // select count(1) from player where xp > 2000
    println(db.renderSql(highXpCount))

    // multiple aggregates
    val aggregates = Player.select.aggregate(p =>
      (p.minBy(_.xp), p.avgBy(_.xp))
    ) // select min(xp), avg(xp) from player
    println(db.renderSql(aggregates))

    // multiple aggregates PER GROUP
    val aggregatesByGameType = Player.select.groupBy(_.gameType)(p =>
      (p.minBy(_.xp), p.avgBy(_.xp))
    ) // select min(xp), avg(xp) from player group by game_type
    println(db.renderSql(aggregatesByGameType))

    // order by
    val byXpOrdering = Player.select.sortBy(_.xp).desc // select * from player order by xp desc
    println(db.renderSql(byXpOrdering))

    // joins
    val join = Player.select
      .join(Game)(_.gameType === _.gameType) // "on player.game_type = game.game_type"
      // ^^ query of (Player, Game)
      .filter { 
        case (player, game) => player.xp > 2000
      }
    println(db.renderSql(join))
    println(db.run(join))
  }
}
