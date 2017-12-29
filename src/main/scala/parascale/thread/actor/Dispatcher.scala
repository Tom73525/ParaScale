/*
 Copyright (c) Ron Coleman

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package parascale.thread.actor

import parascale.thread.actor.Constant._
import org.apache.log4j.Logger
import scala.util.Random

object Dispatcher extends App {
  val LOG =  Logger.getLogger(getClass)

  val config = Config()

    val consumers = (0 until config.numWorkers).foldLeft(List[Worker]()) { (list, n) =>
      val consumer = new Worker(n)

      consumer.start

      consumer :: list
    }

    val ran = new Random

    for(taskno <- 0 until Constant.NUM_TASKS) {
      val task = produce(taskno)

      val index = ran.nextInt(consumers.size)

      consumers(index).send(task)
    }

    consumers.foreach { consumer =>
      consumer.send(DONE)
      consumer.join
  }

  /**
    * Produces a task.
    * @param num Task number
    * @return Task
    */
  def produce(num: Int): Task = {
    Thread.sleep(MAX_PRODUCING)

    LOG.debug("producing task "+num)
    Task(num, MAX_PRODUCING)
  }
}


