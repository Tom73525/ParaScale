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
package parascale.future.perfect

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object FuturePerfectNumberFinder extends App {
  (0 until candidates.length).foreach { index =>
    val num = candidates(index)
    println(num + " is perfect? "+ ask(isPerfectConcurrent,num))
  }

  /**
    * Uses concurrency to determine true if the candidate is perfect.
    * @param candidate Candidate number
    * @return True if candidate is perfect, false otherwise
    */
  def isPerfectConcurrent(candidate: Long): Boolean = {
    val RANGE = 1000000L

    val numPartitions = (candidate.toDouble / RANGE).ceil.toInt

    val futures = for(k <- 0L until numPartitions) yield Future {
      val lower: Long = k * RANGE + 1

      val upper: Long = candidate min (k + 1) * RANGE

      sumOfFactorsInRange_(lower, upper, candidate)
    }

    val total = futures.foldLeft(0L) { (sum, future) =>
      import scala.concurrent.duration._
      val result = Await.result(future, 100 seconds)

      sum + result
    }

    (2 * candidate) == total
  }



  /**
    * Computes the sum of factors in a range using a loop which is robust for large numbers.
    * @param lower Lower part of range
    * @param upper Upper part of range
    * @param number Number
    * @return Sum of factors
    */
  def sumOfFactorsInRange_(lower: Long, upper: Long, number: Long): Long = {
    var index: Long = lower

    var sum = 0L

    while(index <= upper) {
      if(number % index == 0L)
        sum += index

      index += 1L
    }

    sum
  }
}


