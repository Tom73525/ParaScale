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
package parascale.parabond.test

import parascale.parabond.casa.{MongoDbObject, MongoHelper}
import parascale.parabond.util.{Data, Helper, Result}
import parascale.parabond.value.SimpleBondValuator
import scala.util.Random
import parascale.parabond.entry.SimpleBond
import parabond.mr.PORTF_NUM
import parascale.util._

/** Test driver */
object Par04 {
  def main(args: Array[String]): Unit = {
    new Par04 test
  }
}

/**
 * This class uses parallel collections to price n portfolios in the
 * parabond database using the composite coarse-grain algorithm.
 * @author Ron Coleman, Ph.D.
 */
class Par04 {
  /** Initialize the random number generator */
  val ran = new Random(0)   
  
  /** Write a detailed report */
  val details = false

  def test {
    // Set the number of portfolios to analyze
    val n = getPropertyOrElse("n",PORTF_NUM)

    var me =  this.getClass().getSimpleName()
    var outFile = me + "-dat.txt"
    
    var fos = new java.io.FileOutputStream(outFile,true)
    var os = new java.io.PrintStream(fos)
    
    os.print(me+" "+ "N: "+n+" ")

    val details = getPropertyOrElse("details",parseBoolean,false)

    val numCores = Runtime.getRuntime.availableProcessors

    // Build a list the size of the number of cores of portfolio lists
    // Each core will get n/num_cores blocks of portfolios to price
    val blocks = (1 to numCores).foldLeft(List[List[Data]]()) { (portfs, x) =>
      // Build a list of portfolios
      val portf = for(i <- 0 until (n / numCores)) yield Data(ran.nextInt(100000)+1,null, null)

      // portf is a list added as a list element to (not merged with!) the portfs list
      portf.toList :: portfs
    }
    
    // Build the portfolio list
    val t0 = System.nanoTime
    val results = blocks.par.map(price)
    val t1 = System.nanoTime

    // Generate the output report
    if(details)
      println("%6s %10.10s %-5s %-2s".format("PortId","Price","Bonds","dt"))

    val dt1 = results.foldLeft(0.0) { (sum,result) =>
      result.foldLeft(0.0) { (sm,rslt) =>
        sm + (rslt.result.t1 - rslt.result.t0)
      } + sum
    } / 1000000000.0

    val dtN = (t1 - t0) / 1000000000.0

    val speedup = dt1 / dtN


    val e = speedup / numCores

    os.println("dt(1): %7.4f  dt(N): %7.4f  cores: %d  R: %5.2f  e: %5.2f ".
        format(dt1,dtN,numCores,speedup,e))

    os.flush

    os.close

    println(me+" DONE! %d %7.4f".format(n,dtN))
  }

  /**
    * Price a collection of portfolios.
    * @param portfs Portfolios
    * @return Collection of priced portfolios
    */
  def price(portfs: List[Data]) : List[Data] = {
    val outputs = portfs.foldLeft(List[Data]()) { (results, portf) =>
      val t0 = System.nanoTime
      
      val portfId = portf.portfId
      
      val portfsQuery = MongoDbObject("id" -> portfId)

      val portfsCursor = MongoHelper.portfolioCollection.find(portfsQuery)
    
      // Get the bonds ids in the portfolio
      val bondIds = MongoHelper.asList(portfsCursor,"instruments")
    
      // Price each bond and sum all the prices--
      // we could parallel process these using parallel collection which
      // is done by Par05.
      val value = bondIds.foldLeft(0.0) { (sum, id) =>
        // Get the bond from the bond collection
        val bondQuery = MongoDbObject("id" -> id)

        val bondCursor = MongoHelper.bondCollection.find(bondQuery)

        val bond = MongoHelper.asBond(bondCursor)
      
        // Price the bond
        val valuator = new SimpleBondValuator(bond, Helper.curveCoeffs)

        val price = valuator.price
      
        // The price into the aggregate sum
        sum + price
      }    
    
      MongoHelper.updatePrice(portfId,value) 
      
      val t1 = System.nanoTime
    
      Data(portfId,null,Result(portfId,value,bondIds.size,t0,t1)) :: results
    }
 
    outputs
  }  
}