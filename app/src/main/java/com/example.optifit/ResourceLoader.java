package com.example.optifit;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Triple;

public class ResourceLoader {
    private final Resources res;

    public ResourceLoader (Resources res) {
        this.res = res;
    }

    /**
     * Loads the list of words defined in arrays.xml into a list of pairs.
     * The first element is the word.
     * The second is the phonemes used to pronounce the word fluently.
     * @return the list of words as a list of pairs.
     */
    public ArrayList<Pair<String, String>> parseWordList() {
        ArrayList<Pair<String, String>> wordList = new ArrayList<>();
        TypedArray wordArray = res.obtainTypedArray(R.array.word_list);
        List<List<String>> specialList = loadResourcesIntoMap(res, wordArray);

        for (List<String> word : specialList) {
            wordList.add(new Pair<>(word.get(0), word.get(1)));
        }

        wordArray.recycle(); // Important!
        return wordList;
    }

    /**
     * Loads the list of special cases defined in arrays.xml into a list of triples.
     * The first element is the phonemes expected in the case.
     * The second element is the phonemes in the incorrect pronunciation that causes the special case.
     * The third element is the feedback to provide the user in the given special case.
     * @return the list of special cases as a list of triples.
     */
    public ArrayList<Triple<String, String, String>> parseSpecialCasesList() {
        ArrayList<Triple<String, String, String>> specialFeedbackCases = new ArrayList<>();
        TypedArray specialCaseArray = res.obtainTypedArray(R.array.special_feedback_cases);
        List<List<String>> specialList = loadResourcesIntoMap(res, specialCaseArray);

        for (List<String> word : specialList) {
            specialFeedbackCases.add(new Triple<>(word.get(0), word.get(1), word.get(2)));
        }

        specialCaseArray.recycle(); // Important!
        return specialFeedbackCases;
    }

    private List<List<String>> loadResourcesIntoMap(Resources res, TypedArray resouceArray) {
        List<List<String>> result = new ArrayList<>();

        int resourceArrayLength = resouceArray.length();
        for (int i = 0; i < resourceArrayLength; ++i) {
            int id = resouceArray.getResourceId(i, 0);
            if (id > 0) {
                result.add(new ArrayList<>(Arrays.asList(res.getStringArray(id))));
            } else {
                // ToDo: Handle something wrong with the XML
            }
        }

        return result;
    }
}
