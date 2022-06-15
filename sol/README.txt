Taishi Nishizawa and Keigo Hachisuka

- instructions describing how a user would interact with your program.
A user will interact with our program by first choosing the wikipedia database in which they wish to choose.
 The options consist of a BigWiki and MedWiki which as the name implies is the size of the corpus.
 The user will then decide whether to use pagerank or not to calculate the result of their query.
 After these decisions they will run the program and will be prompted to enter a query and the user should enter
 whatever they wish to look up. The search will result in up to top 10 results.

- a brief overview of your design, including how the pieces of your program fit together
Our program will first take in a file which will be indexed. The program will first get each individual page from the
XML file and from each file will retrieve the title, id, and text. The title and text will then be tokenized using
regex to remove unnecessary spaces and characters. The words will then be filtered to remove any stop words and finally
be stemmed to remove any unnecessary parts of the word and return the root. During this process a list of links will
be created for each document. The words then will be counted to calculate the frequency as well as counting how
documents in which the word shows up. The data will then be stored in a hashmap for constant time access later on.
The data will then be used to calculate the tf*idf of each word which will then be stored. Next the pagerank of each
page will be calculated. The special cases such as links to the page itself, repeat links, and and links to things
outside of the corpus were removed when first initializing the list of links. The program will then take into account
if which documents are linking to what (k to j) and then based on that consider the number of links a document is
linking to and calculate the weight. There is also a check distance function which will calculate the distance between
r and r’ and return a boolean of whether the distance is less than 0.001. If so the pagerank calculation will stop and
eturn the rank for the specified page. The id to tile will be stored in titles.txt, the id to ranking in docs.txt and
the relevance of documents to words will be stored in words.txt all of which will be processed by the File IO.
In the querier the three txt files will be read and in the function getResults the process of finding the docs will
take place. The query will first be parsed and tokenized to maintain consistency of the strings.
The program will first calculate the sum of the relevance based on the term frequency of the given words in the query
and find the top 12 documents. If the user does not wish to use the pagerank the top 10 document ids will be returned.
If the user wishes to use pagerank then the top 12 documents scores will have their respective pagerank scores added
and the top 12 will be recalculated. Then the top ten document ids will be returned. In the repl getResults will be
called on the user’s input after the search> prompt which will then the results will be displayed in order.
If there are no results the string “Sorry, there were no results” will be displayed.

- a description of any known bugs in your program
Our program will work for the most part but will run into time and/or space errors. When running Bigwiki the program
will run out of space. If we remove some hashmaps to decrease the amount of space taken then the program will take
longer to run and fall outside of the allowed time limit.


Page Rank Results:
Our pagerank algorithm returns the right results for PageRankWiki.xml, PageRankExample1.xml,
and PageRankExample2.xml with the total sum of the ranks adding up  to 1.

Furthermore, we tested the pagerank results on SmallWiki.xml, MedWiki.xml, and BigWiki.xml, and we got the sum of
the ranks to be very close to 1.
SmallWiki sum: 0.9999999999999972
MedWiki sum: 0.9999999999999873
BigWiki sumi: 1.0000000000000513

We tested our term frequency using the following xml

<xml>
 <page>
   <title>
     apple
   </title>
   <id>
     0
   </id>
   <text>
     Apple apple apple ramen ramen golf golf
   </text>
 </page>

 <page>
   <title>
     banana
   </title>
   <id>
     15
   </id>
   <text>
      banana banana ramen golfing golf ramens
   </text>
 </page>

 <page>
   <title>
     sushi
   </title>
   <id>
     30
   </id>
   <text>
     sushi sushi sushi golf ramen golfing ramen ramen
   </text>
 </page>
</xml>

 We did our calculations for term frequency utilizing simple math thus instead of idf = log(n/n_i) we simply
 calculated n/n_i. The term frequency was calculated as normal and the final product was made into a sum giving
 us relevance = tf + idf. We did this to get more accurate results quicker when doing the math by hand.

This resulted in the following:
Apple: 0->4
Banana: 15-> 4
Sushi: 30 -> 4
Ramen: 0->1.5, 15>1.67, 30->1.75
Golf: 0->1.5, 15>1.67, 30->1.5

We used iterations of this xml such as those with links of different kinds to(with the output being the same every time)
thus being able to check to see if our links were working properly.

Once we figured out that the math and term frequency and page rank was correct we then used the same code in querier
to make sure we got the expected results.
For example the query “apple golf” will return
1. apple
2. sushi

We also tested the results on the results of the given demo, using MedWiki and did a sanity check to make sure our
results were similar. For example the query “hello” in the demo results in

1. Shoma Morita
2. Enjambment
3. Shock site
4. Java (programming language)
5. Luxembourgish language
6. John Woo
7. Mandy Patinkin
8. Forth (programming language)
9. HAL 9000
10. Kareem Abdul-Jabbar

and results in

1. Java (programming language)
2. Shoma Morita
3. Enjambment
4. Forth (programming language)
5. John Woo
6. Shock site
7. Luxembourgish language
8. Mandy Patinkin
9. HAL 9000
10. Kareem Abdul-Jabbar

in our program, thus we know that our program is working as it should.

In a search with pagerank the demo results in

1. Shoma Morita
2. Java (programming language)
3. Enjambment
4. John Woo
5. Shock site
6. Luxembourgish language
7. Forth (programming language)
8. Mandy Patinkin
9. HAL 9000
10. Hermann G?ring

and our program results in:

1. Java (programming language)
2. Shoma Morita
3. Enjambment
4. Forth (programming language)
5. John Woo
6. Shock site
7. Luxembourgish language
8. Mandy Patinkin
9. HAL 9000
10. Kareem Abdul-Jabbar

which serves as another sanity check.