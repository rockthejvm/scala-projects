package com.rockthejvm.guardianscraper

import pureconfig.ConfigReader

case class EmailConfig(
  host: String,
  port: Int,
  user: String,
  password: String
) derives ConfigReader

case class ScraperConfig (
  emailConfig: EmailConfig
) derives ConfigReader
