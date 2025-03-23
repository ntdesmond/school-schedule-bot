package io.github.ntdesmond.serdobot

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import dao.DayScheduleDAO
import dao.postgres.DayScheduleDAOImpl
import io.getquill.JdbcContextConfig
import io.getquill.jdbczio.Quill
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.testcontainers.utility.DockerImageName
import zio.ZIO
import zio.ZLayer
import zio.test.TestEnvironment
import zio.test.ZIOSpec
import zio.test.testEnvironment

type Env = DayScheduleDAO

abstract class SerdobotItSpec extends ZIOSpec[Env]:
  def dataSource: ZLayer[Any, Throwable, DataSource] =
    ZLayer(
      ZIO
        .attempt {
          PostgreSQLContainer(
            dockerImageNameOverride = DockerImageName.parse("postgres:13"),
            databaseName = "serdobot",
            username = "serdobot",
            password = "serdobot",
          )
        }
        .tap(pg => ZIO.attempt(pg.start()))
        .map { pg =>
          val config = new HikariConfig():
            c =>
            c.setJdbcUrl(pg.container.getJdbcUrl)
            c.setUsername(pg.container.getUsername)
            c.setPassword(pg.container.getPassword)

          new HikariDataSource(config)
        },
    )

  def migrations: ZLayer[DataSource, Throwable, Unit] =
    ZLayer(
      for
        ds <- ZIO.service[DataSource]
        flyway <- ZIO.attempt(
          Flyway.configure().dataSource(ds).locations("filesystem:migrations").load(),
        )
        _ <- ZIO.attempt(flyway.migrate())
      yield (),
    )

  override def bootstrap: ZLayer[Any, Any, Env] =
    dataSource >+> migrations >>>
      DayScheduleDAOImpl.layer
