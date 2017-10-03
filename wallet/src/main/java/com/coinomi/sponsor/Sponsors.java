package com.coinomi.sponsor;

import com.coinomi.core.Preconditions;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sponsors {
    private static final Logger log = LoggerFactory.getLogger(Sponsors.class);
    private final HashMap<String, ArrayList<Sponsor>> coinSponsorMap;

    public static class Sponsor {
        public final Integer colorBg;
        public final Integer colorFg;
        public final URI image;
        public final boolean isSponsor;
        public final URI link;
        public final String primary;
        public final String secondary;
        public final double weight;

        private Sponsor(double weight, String primary, String secondary, URI link, URI image, Integer colorFg, Integer colorBg, boolean isSponsor) {
            boolean z = weight >= 0.0d || weight <= 1.0d;
            Preconditions.checkState(z);
            this.weight = weight;
            this.primary = (String) Preconditions.checkNotNull(primary);
            this.secondary = secondary;
            this.link = link;
            this.image = image;
            this.colorFg = colorFg;
            this.colorBg = colorBg;
            this.isSponsor = isSponsor;
        }

        static Sponsor parse(JSONObject sponsorInfo) throws JSONException {
            double weight = parseWeight(sponsorInfo);
            Integer colorFg = null;
            Integer colorBg = null;
            boolean isSponsor = true;
            try {
                if (sponsorInfo.has("colors")) {
                    JSONObject colors = sponsorInfo.getJSONArray("colors").getJSONObject(0);
                    colorFg = getColor(colors, "color_fg");
                    colorBg = getColor(colors, "color_bg");
                }
            } catch (JSONException e) {
            }
            try {
                if (sponsorInfo.has("is_sponsor")) {
                    isSponsor = sponsorInfo.getBoolean("is_sponsor");
                }
            } catch (JSONException e2) {
            }
            return new Sponsor(weight, sponsorInfo.getString("primary"), sponsorInfo.optString("secondary", null), getUri(sponsorInfo, "link"), getUri(sponsorInfo, "image"), colorFg, colorBg, isSponsor);
        }

        private static URI getUri(JSONObject sponsorInfo, String key) {
            URI uri = null;
            try {
                if (sponsorInfo.has(key)) {
                    uri = URI.create(sponsorInfo.optString(key));
                }
            } catch (IllegalArgumentException e) {
            }
            return uri;
        }

        private static double parseWeight(JSONObject sponsorInfo) {
            double weightParsed = sponsorInfo.optDouble("weight", 1.0d);
            if (weightParsed < 0.0d) {
                return 0.0d;
            }
            if (weightParsed > 1.0d) {
                return 1.0d;
            }
            return weightParsed;
        }

        private static Integer getColor(JSONObject colors, String colorName) {
            try {
                return Integer.valueOf(Integer.decode(colors.optString(colorName)).intValue() - 16777216);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private Sponsors(HashMap<String, ArrayList<Sponsor>> coinSponsorMap) {
        this.coinSponsorMap = coinSponsorMap;
    }

    public static Sponsors parse(String sponsorJsonString) throws JSONException {
        JSONObject sponsorsJson = new JSONObject(sponsorJsonString);
        return new Sponsors(parseCoinSponsors(parseSponsors(sponsorsJson.getJSONObject("sponsors")), sponsorsJson.getJSONObject("coins")));
    }

    public Sponsor getRandomSponsor(String id) {
        Sponsor sponsor = null;
        if (this.coinSponsorMap.containsKey(id)) {
            double highestScore = 0.0d;
            Iterator it = ((ArrayList) this.coinSponsorMap.get(id)).iterator();
            while (it.hasNext()) {
                Sponsor candidate = (Sponsor) it.next();
                double candidateScore = Math.random() * candidate.weight;
                if (candidateScore > highestScore) {
                    highestScore = candidateScore;
                    sponsor = candidate;
                }
            }
        }
        return sponsor;
    }

    private static HashMap<String, ArrayList<Sponsor>> parseCoinSponsors(HashMap<String, Sponsor> sponsorsMap, JSONObject coins) throws JSONException {
        HashMap<String, ArrayList<Sponsor>> coinSponsorMap = new HashMap();
        Iterator keys = coins.keys();
        while (keys.hasNext()) {
            String id = keys.next().toString();
            JSONArray sponsorIds = coins.getJSONArray(id);
            ArrayList<Sponsor> sponsorsForCoin = new ArrayList(sponsorIds.length());
            for (int i = 0; i < sponsorIds.length(); i++) {
                String sponsorId = sponsorIds.getString(i);
                Sponsor sponsor = (Sponsor) sponsorsMap.get(sponsorId);
                if (sponsor != null) {
                    sponsorsForCoin.add(sponsor);
                } else {
                    log.warn("Could not find sponsor with id {} for {}, skipping...", sponsorId, id);
                }
            }
            if (sponsorsForCoin.size() > 0) {
                coinSponsorMap.put(id, sponsorsForCoin);
            } else {
                log.warn("No sponsors found for type {}", id);
            }
        }
        return coinSponsorMap;
    }

    private static HashMap<String, Sponsor> parseSponsors(JSONObject sponsors) {
        HashMap<String, Sponsor> sponsorsMap = new HashMap();
        Iterator keys = sponsors.keys();
        while (keys.hasNext()) {
            String id = keys.next().toString();
            try {
                sponsorsMap.put(id, Sponsor.parse(sponsors.getJSONObject(id)));
            } catch (JSONException e) {
                log.warn("Error adding sponsor {}, skipping due to {}", id, e.getMessage());
            }
        }
        return sponsorsMap;
    }
}
