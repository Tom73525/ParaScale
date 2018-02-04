/*
 * Copyright (c) Ron Coleman
 * See CONTRIBUTORS.TXT for a full list of copyright holders.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Scaly Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE DEVELOPERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package parabond.cluster

import org.apache.log4j.Logger
import parabond.mr.PORTF_NUM
import parascale.parabond.util.Constant.NUM_PORTFOLIOS
import parascale.parabond.util.Data
import parascale.util.getPropertyOrElse
import scala.util.Random

/**
  * This object starts the analysis and runs the analysis report.
  */
object CoarseGrainedDrone extends App {
  val LOG = Logger.getLogger(getClass)

  val analysis = new CoarseGrainedDrone analyze

  report(LOG, analysis)
}

/**
  * Prices a block of portfolios per core.
  */
class CoarseGrainedDrone extends Drone {
  /**
    * Prices each portfolio
    * @return
    */
  def naive = new NaiveDrone

  /**
    * Runs the portfolio analyses.
    * @return Analysis
    */
  def analyze: Analysis = {
    // Clock in
    val t0 = System.nanoTime

    // Seed must be same for ever host in cluster as this establishes
    // the randomized portfolio sequence
    val seed = getPropertyOrElse("seed",0)
    Random.setSeed(seed)

    // Size of database
    val size  = getPropertyOrElse("size", NUM_PORTFOLIOS)

    // Shuffled deck of portfolios
    val deck = Random.shuffle(0 to size-1)

    // Number of portfolios to analyze
    val n = getPropertyOrElse("n", PORTF_NUM)

    // Start and end (inclusive) indices in analysis sequence
    val begin = getPropertyOrElse("begin", 0)
    val end = begin + n

    // Indices in the deck we're working on
    // Note: k+1 since portf ids are 1-based
    val indices = for(k <- begin to end) yield Data(deck(k) + 1)

    // Block the indices according to number of cores: each core gets a single clock.
    val numCores = getPropertyOrElse("cores",Runtime.getRuntime.availableProcessors)

    val blksize = n / numCores

    val blocks = for(core <- 0 until numCores) yield {
      val start = core * blksize + begin
      val finish = start + blksize

      indices.slice(start, finish)
    }

    // Run the analysis
    val results = blocks.par.map(price)

    // Need Seq[Data], not ParSeq[Seq[Data]], for reporting and compiler specs
    val flattened = results.flatten

    // Clock out
    val t1 = System.nanoTime

    Analysis(flattened, t0, t1)
  }

  /**
    * Price a collection of portfolios.
    * @param jobs Portfolios
    * @return Collection of priced portfolios
    */

  def price(jobs: Seq[Data]) : Seq[Data] = {
    // We can do this because each job specified by the data passes through the naive map.
    jobs.map(naive.price)
  }
}