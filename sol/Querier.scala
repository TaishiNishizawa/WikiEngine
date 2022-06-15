package sol

import src.{FileIO, IQuerier, PorterStemmer, StopWords}

import java.io.{BufferedReader, InputStreamReader}
import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer}

/**
 * Class for a querier REPL that uses index files built from a corpus.
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if PageRank is to be used to rank results
 */
class Querier(titleIndex: String, documentIndex: String, wordIndex: String,
              usePageRank: Boolean) extends IQuerier {
  // the hashmaps we'll use for querying
  val idsToTitles: HashMap[Int, String] = new HashMap()
  val idsToPageRanks: HashMap[Int, Double] = new HashMap()
  val wordsToDocsToRelevance: HashMap[String, HashMap[Int, Double]] = new HashMap()

  // fill in the three hashmaps
  FileIO.readTitles(titleIndex, idsToTitles)
  FileIO.readDocsFile(documentIndex, idsToPageRanks)
  FileIO.readWordsFile(wordIndex, wordsToDocsToRelevance)

  override def getResults(query: String): List[Int] = {
    //splits the query by space
    val splitQuery = query.split(" ");
    //list for the query without stop words
    val queryWithoutSW: ListBuffer[String] = new ListBuffer[String]()
    //filters out the stop words
    for (word <- splitQuery) {
      if (!StopWords.isStopWord(word)) {
        queryWithoutSW.addOne(word)
      }
    }
    //stems the words in the query
    val lstOfWords: ListBuffer[String] = queryWithoutSW.map(n => PorterStemmer.stem(n.toLowerCase()));

    /**
     * calculates the relevance of each document, where relevance is the product of tf and idf.
     *
     * @param pageId - the id of the page
     * @return - the sum of the each word relevance for the page, as a Double
     */
    def calcSumRelevance(pageId: Int): Double = {
      var summation: Double = 0.0;
      for (word <- lstOfWords) {
        if (wordsToDocsToRelevance.contains(word)) {
          if (wordsToDocsToRelevance(word).contains(pageId)) {
            val relevance: Double = wordsToDocsToRelevance(word)(pageId)
            summation = summation + relevance
          } else {
            val relevance: Double = 0.0
            summation = summation + relevance
          }
        }
      }
      summation
    }

    val pageRelevanceHM: mutable.HashMap[Int, Double] = new mutable.HashMap()
    val pageRelevanceHMCopy: mutable.HashMap[Int, Double] = new mutable.HashMap()
    // stores "relevance" of each page
    for (page <- idsToTitles) {
      val pageRel: Double = calcSumRelevance(page._1)
      pageRelevanceHM.addOne(page._1, pageRel)
      pageRelevanceHMCopy.addOne(page._1, pageRel);
    }
    var counter = 0;
    var lstOfTop: List[Int] = List()
    // stores the ids of the documents with ids top 12 relevance
    while (counter < 12) {
      //checks to see if the hashmap is empty
      if (pageRelevanceHM.nonEmpty) {
        val page = pageRelevanceHM.maxBy(_._2)
        if (page._2 != 0.0) {
          lstOfTop :+= (page._1) // append, so that the highest come first
          pageRelevanceHM.remove(page._1)
        }
      }
      counter = counter + 1
    }
    if (usePageRank) {
      // calculate pageRank
      var lstOfPageRankRelCombined: List[(Int, Double)] = List()
      var lstToReturn: List[Int] = List()
      for (page <- lstOfTop) {
        //add pagerank score and relevance score
        val combined = pageRelevanceHMCopy(page) * 0.35 + idsToPageRanks(page) * 0.65
        lstOfPageRankRelCombined ::= (page, combined)
      }
      // sort list in descending order
      lstOfPageRankRelCombined = lstOfPageRankRelCombined.sortWith(_._2 > _._2)
      for (page <- lstOfPageRankRelCombined) {
        lstToReturn :+= page._1
      }
      //take the 10
      val lstOfTopPR: List[Int] = lstToReturn.takeWhile(n => lstToReturn.indexOf(n) < 10)
      lstOfTopPR
    } else {
      //take the top 10
      val lstOfTopTen: List[Int] = lstOfTop.takeWhile(n => lstOfTop.indexOf(n) < 10)
      lstOfTopTen
    }
  }

  override def runRepl(): Unit = {
    val inputReader = new BufferedReader(new InputStreamReader(System.in))
    print("search> ")
    var terms: String = inputReader.readLine()
    while (terms != null && terms != ":quit") {
      val lstOfIds = getResults(terms)
      //check if there are not reuslts
      if (lstOfIds.isEmpty) {
        println("Sorry, there were no results")
      } else {
        //print results
        for (i <- lstOfIds.indices) {
          println(i + 1 + ". " + idsToTitles(lstOfIds(i)))
        }
      }
      print("search> ")
      terms = inputReader.readLine();
    }
    inputReader.close()
  }
}


object Querier {
  /**
   * Runs the querier REPL.
   *
   * @param args args of the form [--pagerank (optional), titlesIndex, docsIndex,
   *             wordsIndex]
   */
  def main(args: Array[String]): Unit = {
    if (args(0) == "--pagerank") {
      val queR = new Querier(args(1), args(2), args(3), true)
      queR.runRepl()
    } else {
      val queR = new Querier(args(0), args(1), args(2), false)
      queR.runRepl()
    }
  }
}