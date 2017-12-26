package scalaparallel.thread.actor

case class Config(numWorkers: Int, numTasks: Int)

object Config {
  def apply(): Config = {
    import java.io.FileInputStream

    val properties = System.getProperties

    properties.load(new FileInputStream("parascale.conf"))

    val workersProp = System.getProperty("parascale.workers")

    val numWorkers = workersProp match {
      case prop:String =>
        prop.toInt

      case null =>
        Runtime.getRuntime.availableProcessors
    }

    val tasksProp = System.getProperty("parascale.tasks")
    val numTasks = workersProp match {
      case prop:String =>
        prop.toInt

      case null =>
        Constant.NUM_TASKS
    }

    Config(numWorkers, numTasks)
  }
}
