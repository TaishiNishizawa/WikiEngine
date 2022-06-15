package sol

import src.{FileIO, PorterStemmer, StopWords}

import java.io.{BufferedWriter, FileWriter}
import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer}
import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

/**
 * Provides an XML indexer, produces files for a querier
 *
 * @param inputFile - the filename of the XML wiki to be indexed
 */
class Indexer(val inputFile: String) {
  // store title and text for each page id
  val idToTitleText: mutable.HashMap[Int, (String, String)] = new mutable.HashMap()
  // store title to id
  val titleToId: mutable.HashMap[String, Int] = new mutable.HashMap()

  /**
   * This method gets the pages of the xml file, in NodeSeq format
   *
   * @return the pages of the xml file, as NodeSeq
   */
  def getSequences(): NodeSeq = {
    val root: Node = xml.XML.loadFile(inputFile);
    // get pages
    root \ "page"
  }
  // fill in hashmaps idToTitleText and titleToId
  for (pq <- getSequences()) {
    val iid: Int = (pq \ "id").text.trim.toInt
    val title: String = (pq \ "title").text.trim
    val text: String = (pq \ "text").text.trim
    val words = title ++ " " ++ text;
    idToTitleText.addOne(iid, (title, words))
    titleToId.addOne(title, iid)
  }

  /**
   * creates a hashmap of the id to a list of links and a hashMap of words to their frequency
   *
   * @return a hashmap of the id to a list of links and a hashMap of words to their frequency
   */
  def makeHashOfAllHashes(): mutable.HashMap[Int, (ListBuffer[String], mutable.HashMap[String, Double])] = {
    // the hashmap to return
    val hashOfAllHashes: mutable.HashMap[Int, (ListBuffer[String], mutable.HashMap[String, Double])] =
      new mutable.HashMap()
    val regex: Regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")
    for (pq <- getSequences()) {
      val id: Int = (pq \ "id").text.trim.toInt
      val matchesIterator = regex.findAllMatchIn(idToTitleText(id)._2)
      val matchesList: List[String] = matchesIterator.toList.map { aMatch => aMatch.matched }
      // lists of all words that is not a link or wrods extracted from a link
      val withOutSW: ListBuffer[String] = new ListBuffer()
      // list of links for a given page
      val linkList: ListBuffer[String] = new ListBuffer();
      //list of the strings in the links
      val linkWords: ListBuffer[String] = new ListBuffer()
      for (i <- matchesList) {
        // checks for double brackets as that means the string is a link
        if (i.startsWith("[[")) {
          //extracts the words from the links
          val slicedWord = i.slice(2, i.length - 2)
          //checks to see if a link has |
          if (slicedWord.contains("|")) {
            //get the string after the | and split by space
            val lstOfWordsFromBar = slicedWord.split("\\Q|\\E")
            //checks for repeat links, checks for links to outside the corpus, checks for link to itself then adds link
            if (!linkList.contains(lstOfWordsFromBar.head) && titleToId.contains(lstOfWordsFromBar.head)
              && idToTitleText(id)._1 != lstOfWordsFromBar.head) {
              linkList.addOne(lstOfWordsFromBar.head)
            }
            //add the words to the list
            for (str <- lstOfWordsFromBar.tail) {
              val barSpaceSplit = str.split(" ")
              //add the words to the list of words
              for (word <- barSpaceSplit) {
                linkWords.addOne(word)
              }
            }
            //checks if the link contains a colon
          } else if (slicedWord.contains(":")) {
            //checks for repeat links, checks for links to outside the corpus, checks for link to itself the adds link
            if (!linkList.contains(slicedWord)
              && titleToId.contains(slicedWord)
              && idToTitleText(id)._1 != slicedWord) {
              linkList.addOne(slicedWord)
            }
            //gets the strings before and after the colon
            val lstOfWordsFromColon = slicedWord.split(":");
            // for each string split by space to get each individual word
            for (str <- lstOfWordsFromColon) {
              val colonSpaceSplit = str.split(" ")
              //add the words to the list of words
              for (word <- colonSpaceSplit) {
                linkWords.addOne(word)
              }
            }
            //if the link contains no | or : just split by space
          } else {
            //checks for repeat links, checks for links to outside the corpus, checks for link to itself the adds link
            if (!linkList.contains(slicedWord)
              && titleToId.contains(slicedWord)
              && idToTitleText(id)._1 != slicedWord) {
              linkList.addOne(slicedWord)
            }
            //splits the string by space
            val splitBySpace = slicedWord.split(" ")
            //adds words to the list words
            for (word <- splitBySpace) {
              linkWords.addOne(word);
            }
          }
          // if a string is not a link then check to see if its a stopword then adds it to the list of words
        } else {
          if (!StopWords.isStopWord(i)) {
            withOutSW.addOne(i)
          }
        }
      }
      /* for the list on words from the links perform regex and filter out the stop words and add the words to the
      list of words
      */
      for (str <- linkWords) {
        val mI = regex.findAllMatchIn(str);
        val mL: List[String] = mI.toList.map { aMatch => aMatch.matched }
        for (word <- mL) {
          if (!StopWords.isStopWord(word)) {
            withOutSW.addOne(word)
          }
        }
      }
      // stem all words
      val stemmedTxt = withOutSW.map(n => PorterStemmer.stem(n.toLowerCase()))
      val wordToFreq: mutable.HashMap[String, Double] = new mutable.HashMap()
      //max number of occurences of a word in a page
      var maxCount: Double = 0.0
      // calculates the frequency of each words while calculating the words with the highest frequency
      for (word <- stemmedTxt) {
        if (wordToFreq.contains(word)) {
          if (wordToFreq(word) + 1.0 > maxCount) {
            maxCount = wordToFreq(word) + 1.0;
          }
          wordToFreq(word) = wordToFreq(word) + 1.0;
        } else {
          wordToFreq.addOne(word, 1)
          // if maxCount is 0 then change it to 1
          if (maxCount == 0.0) {
            maxCount = 1.0
          }
        }
      }
      // add maxCount as last item to linkList (but will get removed in termFreqCalc)
      linkList.addOne(maxCount.toString);
      //add the list of links and the hashmap of words to frequency to the hashmap
      hashOfAllHashes.addOne(id, (linkList, wordToFreq))
    }
    // delete the texts of idToTitleText
    for (doc <- idToTitleText) {
      idToTitleText(doc._1) = (idToTitleText(doc._1)._1, "")
    }
    hashOfAllHashes
  }

  val hashOfAllHashes: mutable.HashMap[Int, (ListBuffer[String], mutable.HashMap[String, Double])] =
    makeHashOfAllHashes()

  /**
   * This calculates how many times a word appears throughout the entire corpus, stored as a hashmap.
   *
   * @return - a hash map with the mapping of word -> number of appearances in corpus (ie, all documents)
   */
  def calcTotalWordAppearance(): mutable.HashMap[String, Int] = {
    // instantiate the hashmap to ultimately return
    val wordsAppearanceAllPages: mutable.HashMap[String, Int] = new mutable.HashMap()
    // stores how many times (documents) each word appears
    for (page <- hashOfAllHashes) {
      for (wdFreqPair <- page._2._2) {
        if (wordsAppearanceAllPages.contains(wdFreqPair._1)) {
          wordsAppearanceAllPages(wdFreqPair._1) = wordsAppearanceAllPages(wdFreqPair._1) + 1
        } else {
          wordsAppearanceAllPages.addOne(wdFreqPair._1, 1)
        }
      }
    }
    wordsAppearanceAllPages
  }

  /**
   * creates the hashtable containing the relevance of each word in a hashmap called wordRelevanceHashMap.
   * This hashmap is what gets used to fill our wordsIndex file.
   *
   * @param wordsApp - the hashmap containing the total appearance (in corpus) of every word in corpus
   * @return - the hashmap with the mapping word -> Hashmap(documentId, relevance)
   */
  def createWordRelevanceHashMap(wordsApp: mutable.HashMap[String, Int]):
  mutable.HashMap[String, mutable.HashMap[Int, Double]] = {
    // create hashMap that we ultimately want to return
    val wordRelHashMap: mutable.HashMap[String, mutable.HashMap[Int, Double]] = new mutable.HashMap()
    // calculate relevance for each document, and store that in wordRelevanceHashMap
    for (page <- hashOfAllHashes) {
      val pageId = page._1
      val maxCount = page._2._1.last.toDouble.toInt
      for (wdFreqPair <- page._2._2) {
        // frequency is how many times the word appears in page
        val (word, frequency) = wdFreqPair
        // calculate tf,idf
        val tf = frequency / maxCount.toDouble;
        val idf = math.log(hashOfAllHashes.size.toDouble / wordsApp(wdFreqPair._1).toDouble)
        // calculate relevance
        val relevance = tf * idf
        // add relevance to corresponding place in wordRelevanceHashMap
        // if word already exists in hash, then add another document-to-relevance in the inner hashmap
        if (wordRelHashMap.contains(word)) {
          val docToRelevance: mutable.HashMap[Int, Double] = wordRelHashMap(word)
          wordRelHashMap(word) = docToRelevance.addOne(pageId, relevance)
        } else {
          // word doesn't exist in hash, so add it.
          val docToRelevance: mutable.HashMap[Int, Double] = mutable.HashMap(pageId -> relevance)
          wordRelHashMap.addOne(word, docToRelevance)
        }
      }
    }
    // return the now updated word-to-(id-to-relevance) hash map
    wordRelHashMap
  }

  /**
   * This helper method gets called by pageRank
   *
   * @param pageRankHshMpR - the hashmap with the mapping title to rank and rank-dash. Gets mutated in this method.
   */
  def pageRankHelper(pageRankHshMpR: mutable.HashMap[String, (Double, Double)]) = {
    /**
     * checks to see if the distance between r and r' is smaller than the target distance 0.001
     * Gets called in the while loop that keeps running as long as distance is still larger than 0.001
     *
     * @return - a boolean indicating whether the distance between r and r' is smaller than 0.001
     */
    def checkDistance(): Boolean = {
      var summationOfSquared = 0.0;
      for (page <- pageRankHshMpR) {
        // calculate Euclidean distance
        summationOfSquared = summationOfSquared + math.pow(page._2._1 - page._2._2, 2)
      }
      val euclidDist = math.sqrt(summationOfSquared)
      euclidDist > 0.001
    }
    // while distance(r, r') > delta, keep calculating
    while (checkDistance()) {
      for (page <- pageRankHshMpR) {
        val (r, rDash) = page._2
        // set r <- r'
        pageRankHshMpR(page._1) = (rDash, rDash)
      }
      for (pageJ <- pageRankHshMpR) {
        // r'(pageJ) = 0.0
        pageRankHshMpR(pageJ._1) = (pageJ._2._2, 0.0)
        for (pageK <- pageRankHshMpR) {
          /*
            calculate weight(k,j), or the weight that k gives to j
            if k links to j or not
            access id from title using titleToId hashmap.
              => access list of links from that id
              => check linkList and see if j exists in the list
           */
          var weight: Double = 0.15 / titleToId.size.toDouble
          val titleOfK = pageK._1
          val titleOfJ = pageJ._1
          val lstOfLinksFromK = hashOfAllHashes(titleToId(titleOfK))._1
          if (titleOfJ == titleOfK) {
            weight = weight + 0.0;
          }
          else if (lstOfLinksFromK.isEmpty) {
            // k doesn't link to anything
            weight = weight + (0.85) / (titleToId.size.toDouble - 1.0)
          } else if (lstOfLinksFromK.contains(titleOfJ)) {
            // k links to j
            weight = weight + (0.85) / (lstOfLinksFromK.size.toDouble)
          } else {
            weight = weight + 0.0
          }
          val (rJ, rDashJ) = pageRankHshMpR(pageJ._1)
          val rK = pageRankHshMpR(pageK._1)._1
          // r'(pageJ) = r'(pageJ) + weight(pageK, pageJ) * r(pageK)
          pageRankHshMpR(pageJ._1) = (rJ, rDashJ + weight * rK)
        }
      }
    }
  }

  // create new HashMap with page (title) -> r (prev), r' (curr)
  val pageRankHshMpR: mutable.HashMap[String, (Double, Double)] = new mutable.HashMap()

  /**
   * calculates the pageRank of each page, calling the helper method pageRankHelper to do the actual calculating
   *
   * @param pages - the hashmap hashOfAllHashes gets fed in
   * @return - returns the hashMap containing the rank of each
   */
  def pageRank(pages: mutable.HashMap[Int, (ListBuffer[String], mutable.HashMap[String, Double])])
  : mutable.HashMap[String, (Double, Double)] = {
    // initialize every rank in r to be 0, r' to be 1/n
    for (page <- hashOfAllHashes) {
      // remove last item of linkList, which was the maxCount
      page._2._1.remove(page._2._1.length - 1)
      val title = idToTitleText(page._1)._1
      pageRankHshMpR.addOne(title, (0.0, 1.0 / idToTitleText.size.toDouble))
    }
    // pageRankHelper updates the pageRankHshMpR
    pageRankHelper(pageRankHshMpR)
    pageRankHshMpR
  }

  /**
   * Writes the titles index for the corpus to a file, storing the mapping
   * between document id and title, formatted as follows:
   *
   * docId1::title1
   * docId2::title2
   * ...
   *
   * @param titlesFile  the file to write the titles index to
   * @param idsToTitles a HashMap from document id to document title and text.
   */
  def writeTitlesFile(titlesFile: String, idsToTitles: HashMap[Int, (String, String)]) {
    val titleWriter = new BufferedWriter(new FileWriter(titlesFile))
    for ((id, (title, _)) <- idsToTitles) {
      titleWriter.write(id + "::" + title + "\n")
    }
    titleWriter.close()
  }

  /**
   * Writes the docs index for the corpus to a file, storing the mapping
   * between document id and PageRank, formatted as follows:
   *
   * docId1 pageRank1
   * docId2 pageRank2
   * ...
   *
   * @param docsFile       the file to write the docs index to
   * @param idsToPageRanks a HashMap from document id to PageRank (of old rank and new rank)
   */
  def writeT0Docs(docsFile: String,
                  idsToPageRanks: HashMap[String, (Double, Double)]) {
    val docWriter = new BufferedWriter(new FileWriter(docsFile))
    for ((title, (_, currentRank)) <- idsToPageRanks) {
      docWriter.write(titleToId(title) + " " + currentRank + "\n")
    }
    docWriter.close()
  }
}

object Indexer {
  /**
   * Processes a corpus and writes to index files.
   *
   * @param args args of the form [WikiFile, titlesIndex, docsIndex, wordsIndex]
   */
  def main(args: Array[String]): Unit = {
    val iDXer = new Indexer(args(0))
    // call the methods in Indexer to fit with our data structure
    /**
     * NOTE: THE ORDERING OF THESE CALLS MATTER HERE, DUE TO MUTABILITY
     */
    iDXer.writeTitlesFile(args(1), iDXer.idToTitleText)
    FileIO.writeWordsFile(args(3), iDXer.createWordRelevanceHashMap(iDXer.calcTotalWordAppearance()))
    iDXer.writeT0Docs(args(2), iDXer.pageRank(iDXer.hashOfAllHashes))
  }
}