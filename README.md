# WikiEngine

## About
A search engine that shows the top ten Wikipedia pages based on a keyword. 
Parses the pages represented as XML files using Regex, index them using the TF-IDF scoring and the PageRank algorithm, and query. 
Optimized for runtime-and-space tradeoff. 


## Indexing

### Determining the relevance of documents to term using TF-IDF 
Processing documents as described (parsing, tokenizing, and stemming) converts documents from text into a sequence of terms. Likewise, a query (after parsing, tokenizing, and stemming) is a sequence of terms. Consequently, to score the relevance of a document to a query, we compare these two sequences of terms. Similarity metrics used by most practical search engines capture two key ideas:

    A term that appears many times within a document is likely to be more relevant to that document than a term that appears fewer times. This notion is called the Term Frequency.
    A term that occurs in a fewer documents in the corpus is likely to be a better signal of relevance when it occurs than a term that appears in many documents. This notion is called Inverse Document Frequency.

#### Term Frequency 
Let cij be the number of times that term i appears in document j. Then, let aj=maxicij be the number of occurrences of the most frequently occurring term in document j. We then define the term frequency for a given term as tfij=cij/aj.

#### Inverse Document Frequency
Let i be a term in the corpus, n be the total number of documents, and ni be the number of documents that contain term i. 
Then, we represent the idf as follows: 
idf_i = logN-logN_i

#### Scoring Relevance of Documents to Terms
Given a term i (from anywhere in the corpus) and a document j, we calculate the relevance score of j to i to be the normalized term frequency of i in j multiplied by the inverse document frequency of i. So, 
relevance_ij=(tf)ij∗idf_i

### PageRank: Determining Document Authority
The pagerank algorithm ranks pages based on the links among them (without considering the content). Pages with high scores (i.e., page ranks) are then thought of as authoritative. Given a query, the most authoritative documents related to the words in the query are returned.
Here are the key principles underlying the design of PageRank:
1. The more pages that link to a page j, the more authoritative j should be.
2. The more authoritative those pages are, the still more authoritative j should be. 
3. The fewer links those pages have to pages other than j, the more authoritative j should be.
4. The closer j is to another page k (measured by the number of links that must be followed to get from j to k), the more k’s score should influence j’s.

PageRank uses these principles to compute the authority of each document. The algorithm works roughly as follows: Each page’s authority is a number between 0 and 1, where the total authority across all documents always equals 1. The algorithm starts by assuming that all documents have the same authority. Then, following the principles, the algorithm updates the authorities based on the links between pages. Since the authority of one page can affect that of another, this process of refining authorities repeats until the authorities stabilize across all documents (the underlying mathematics guarantee eventual stabilization). It is important that the total authority in the system is conserved, so at any point in the converging sequence of page ranks, the total authority in the system will always sum to 1.

#### Weighting
In order to determine the rank of a page, we need to first numerically represent the linking relationship between pages as a set of weights. This will help achieve the third principle, and, when combined with the remaining math in this section, the fourth principle.

Weight captures the impact of links from one page to another. Let k
and j be pages, where k has a link to j. We denote the weight that k gives to j as w_kj.

The weight is defined by the following formula, where n
is the total number of pages and nk is the number of (unique) pages that k links to. ϵ is a small number (you should use ϵ=0.15).
w_kj= ϵ/n+(1−ϵ)/n_k if k links to j; ϵ/n otherwise

Special cases: There are a few special cases to handle when defining the weights wkj (before adjusting those weights by ϵ):

    Links from a page to itself are ignored.
    Links to pages outside the corpus are ignored.
    Pages that link to nothing can be considered to link (once) to everywhere except to themselves.
    Multiple links from one page to another should be treated as a single link.

#### Computing rank 
we will compute the ranks for the pages as one step of the PageRank algorithm, and then use those values when computing the next step of the algorithm, until we converge to some stable ranking. We first define the math for a single iteration (step) of this algorithm.

For a page j, the rank r_j on the i-th iteration is defined as follows (here, superscripts with angle-brackets (⟨⟩) denote the iteration number, not an exponent):
r⟨i+1⟩j=∑kwkjr⟨i⟩k

In the base case, we initialize the ranks as:
r⟨0⟩j=1/n

#### Converging on ranks
We repeat the computation in above equation until the computation converges, that is, the distance between two iterations is a sufficiently small amount.
For our distance metric, we use the Euclidean distance between two vectors. 
Once this distance is sufficiently small (say less than δ = 0.001), we declare the rankings stable, and output r⟨i+1⟩ as our final ranking.

#### Pseudocode for PageRank
pageRank(pages):

      initialize every rank in r to be 0
      initialize every rank in r' to be 1/n
        while distance(r, r') > delta:
          r <- r'
          for j in pages:
              r'(j) = 0
              for k in pages:
                  r'(j) = r'(j) + weight(k, j) * r(k)

## Querying
The second big piece of the search engine is the Querier. The Querier does the following:
1. Parse in arguments for the index files and an optional argument that says to use PageRank
2. Run a (Read-Eval-Print Loop) that takes in and processes search queries
3. Scores documents against queries based on the term relevance and PageRank (if specified) index files
