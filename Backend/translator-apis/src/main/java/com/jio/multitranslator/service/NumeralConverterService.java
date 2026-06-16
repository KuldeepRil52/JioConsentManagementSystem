package com.jio.multitranslator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NumeralConverterService {

    private static final Map<String, String[]> LANGUAGE_DIGITS = createDigitMap();

    // Reverse lookup: digit char → index
    private static final Map<String, Map<Character, Integer>> LANGUAGE_INDEX_MAP = buildIndexMaps();

    private static Map<String, String[]> createDigitMap() {
        Map<String, String[]> map = HashMap.newHashMap(20);

        map.put("en", new String[]{"0","1","2","3","4","5","6","7","8","9"});
        map.put("hi", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("bn", new String[]{"০","১","২","৩","৪","৫","৬","৭","৮","৯"});
        map.put("ta", new String[]{"௦","௧","௨","௩","௪","௫","௬","௭","௮","௯"});
        map.put("te", new String[]{"౦","౧","౨","౩","౪","౫","౬","౭","౮","౯"});
        map.put("kn", new String[]{"೦","೧","೨","೩","೪","೫","೬","೭","೮","೯"});
        map.put("ml", new String[]{"൦","൧","൨","൩","൪","൫","൬","൭","൮","൯"});
        map.put("gu", new String[]{"૦","૧","૨","૩","૪","૫","૬","૭","૮","૯"});
        map.put("mr", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("or", new String[]{"୦","୧","୨","୩","୪","୫","୬","୭","୮","୯"});
        map.put("pa", new String[]{"੦","੧","੨","੩","੪","੫","੬","੭","੮","੯"});
        map.put("as", new String[]{"০","১","২","৩","৪","৫","৬","৭","৮","৯"});
        map.put("ne", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("sd", new String[]{"۰","۱","۲","۳","۴","۵","۶","۷","۸","۹"});
        map.put("ks", new String[]{"۰","۱","۲","۳","۴","۵","۶","۷","۸","۹"});
        map.put("doi", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("gom", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("mai", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("sa", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("sat", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("brx", new String[]{"०","१","२","३","४","५","६","७","८","९"});
        map.put("mni", new String[]{"꯰","꯱","꯲","꯳","꯴","꯵","꯶","꯷","꯸","꯹"});

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Map<Character, Integer>> buildIndexMaps() {
        Map<String, Map<Character, Integer>> indexMap = HashMap.newHashMap(LANGUAGE_DIGITS.size());
        for (Map.Entry<String, String[]> entry : LANGUAGE_DIGITS.entrySet()) {
            Map<Character, Integer> charToIndex = HashMap.newHashMap(10);
            String[] digits = entry.getValue();
            for (int i = 0; i < digits.length; i++) {
                charToIndex.put(digits[i].charAt(0), i);
            }
            indexMap.put(entry.getKey(), Collections.unmodifiableMap(charToIndex));
        }
        return Collections.unmodifiableMap(indexMap);
    }

    /**
     * Convert numerals from any source language to target language.
     */
    public String convert(String text, String sourceLang, String targetLang) {
        if (text == null || text.isEmpty() || sourceLang == null || targetLang == null) {
            return text;
        }
        if (!LANGUAGE_DIGITS.containsKey(sourceLang) || !LANGUAGE_DIGITS.containsKey(targetLang)) {
            return text;
        }
        if (sourceLang.equals(targetLang)) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        Map<Character, Integer> sourceIndexMap = LANGUAGE_INDEX_MAP.get(sourceLang);
        String[] targetDigits = LANGUAGE_DIGITS.get(targetLang);

        for (char c : text.toCharArray()) {
            Integer digitIndex = sourceIndexMap.get(c);
            if (digitIndex != null) {
                result.append(targetDigits[digitIndex]);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Batch convert multiple texts.
     *
     * @param texts       list of texts to convert
     * @param sourceLang  source language code
     * @param targetLang  target language code
     * @return list of converted texts, or empty list if input is null
     */
    public List<String> convertAll(List<String> texts, String sourceLang, String targetLang) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return texts.stream()
                .map(t -> convert(t, sourceLang, targetLang))
                .toList();
    }
}
