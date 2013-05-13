#!/usr/bin/python
import csv
import sys
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn import datasets
from sklearn.svm import LinearSVC
from sklearn import random_projection

from sklearn import decomposition

import numpy

def main():
    train_in = sys.argv[1]
    dim = int(sys.argv[2])
    cutoff = int(sys.argv[3])

    
    train_csv = csv.DictReader(open(train_in,'rb'),delimiter='\t')

    transformer = random_projection.GaussianRandomProjection()

#data
    labels = []
    corpus = []
    

    


 
    for line in train_csv:
        labels.append(line['label'].rstrip("\n"))
        corpus.append(line['text'].rstrip("\n"))
        

    vectorizer = CountVectorizer(min_df=cutoff)
    
    
    X = vectorizer.fit_transform(corpus)

    V = len( vectorizer.get_feature_names())

    vectorized = numpy.empty((len(labels),V))

    pca = decomposition.PCA()
    
    pca.n_components = dim

    for i,c in enumerate(corpus):        
        tmp = vectorizer.transform([c]).toarray()
        
        
        vectorized[i] = tmp[0]

    
       
                          
    reduced = pca.fit_transform(vectorized)

    print str(len(labels)) + " " + str(pca.n_components)
    for i,n in enumerate(labels):
        print str(i) + " " + " ".join(map(lambda x: str(x),reduced[i]))

        

def accuracy(labels,vectorized,classifier):
    count = 0
    total = 0
    for l,v in zip(labels,vectorized):
        if l ==  classifier.predict(v)[0]:
            count += 1
        total += 1
    return float(count)/total
    

if __name__ == "__main__":
    main()
