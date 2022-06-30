package scopttest

object Issue264 extends verify.BasicTestSuite {
  private case class Issue264Config(
      cmd: Option[String] = None,
      clientName: Option[String] = None,
      id: Option[String] = None
  )

  test("supports two levels of nested commands with arguments") {
    import scopt.OParser
    val builder = OParser.builder[Issue264Config]
    val parser = {
      import builder._
      OParser.sequence(
        cmd("clients")
          .action((_, c) => c.copy(cmd = Option("clients")))
          .children(
            cmd("add")
              .action((_, c) => c.copy(cmd = c.cmd.map(cmd => s"${cmd}_add")))
              .children(
                arg[String]("<clientName>")
                  .action((x, c) => c.copy(clientName = Option(x)))
              ),
            cmd("remove")
              .action((_, c) => c.copy(cmd = c.cmd.map(cmd => s"${cmd}_remove")))
              .children(
                arg[String]("<clientId>")
                  .action((x, c) => c.copy(id = Option(x)))
              )
          )
      )
    }

    OParser.parse(parser, Seq("clients", "add", "clientName"), Issue264Config()) match {
      case Some(config) =>
        assert(config.cmd == Option("clients_add"))
        assert(config.clientName == Option("clientName"))
        assert(config.id.isEmpty)
      case None =>
        fail(s"could not parse command line arguments")
    }
  }
}
