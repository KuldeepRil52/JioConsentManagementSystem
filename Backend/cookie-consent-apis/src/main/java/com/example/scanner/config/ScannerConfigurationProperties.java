package com.example.scanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scanner")
public class ScannerConfigurationProperties {

    private int maxRedirects = 15;
    private int navTimeoutSeconds = 60;
    private String navigationWait = "networkidle";
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private Interaction interaction = new Interaction();
    private Consent consent = new Consent();
    private Embedded embedded = new Embedded();
    private Cookie cookie = new Cookie();
    private Browser browser = new Browser();
    private Privacy privacy = new Privacy();

    // Getters and setters
    public int getMaxRedirects() { return maxRedirects; }
    public void setMaxRedirects(int maxRedirects) { this.maxRedirects = maxRedirects; }

    public int getNavTimeoutSeconds() { return navTimeoutSeconds; }
    public void setNavTimeoutSeconds(int navTimeoutSeconds) { this.navTimeoutSeconds = navTimeoutSeconds; }

    public String getNavigationWait() { return navigationWait; }
    public void setNavigationWait(String navigationWait) { this.navigationWait = navigationWait; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Interaction getInteraction() { return interaction; }
    public void setInteraction(Interaction interaction) { this.interaction = interaction; }

    public Consent getConsent() { return consent; }
    public void setConsent(Consent consent) { this.consent = consent; }

    public Embedded getEmbedded() { return embedded; }
    public void setEmbedded(Embedded embedded) { this.embedded = embedded; }

    public Cookie getCookie() { return cookie; }
    public void setCookie(Cookie cookie) { this.cookie = cookie; }

    public Browser getBrowser() { return browser; }
    public void setBrowser(Browser browser) { this.browser = browser; }

    public Privacy getPrivacy() { return privacy; }
    public void setPrivacy(Privacy privacy) { this.privacy = privacy; }

    public static class Interaction {
        private boolean enabled = true;
        private Scroll scroll = new Scroll();
        private Click click = new Click();
        private int delayMs = 2000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Scroll getScroll() { return scroll; }
        public void setScroll(Scroll scroll) { this.scroll = scroll; }

        public Click getClick() { return click; }
        public void setClick(Click click) { this.click = click; }

        public int getDelayMs() { return delayMs; }
        public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

        public static class Scroll {
            private boolean enabled = true;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }

        public static class Click {
            private boolean enabled = true;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
    }

    public static class Consent {
        private Handling handling = new Handling();

        public Handling getHandling() { return handling; }
        public void setHandling(Handling handling) { this.handling = handling; }

        public static class Handling {
            private boolean enabled = true;
            private int timeoutSeconds = 10;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        }
    }

    public static class Embedded {
        private Content content = new Content();
        private Iframe iframe = new Iframe();
        private Analytics analytics = new Analytics();

        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }

        public Iframe getIframe() { return iframe; }
        public void setIframe(Iframe iframe) { this.iframe = iframe; }

        public Analytics getAnalytics() { return analytics; }
        public void setAnalytics(Analytics analytics) { this.analytics = analytics; }

        public static class Content {
            private boolean processing = true;

            public boolean isProcessing() { return processing; }
            public void setProcessing(boolean processing) { this.processing = processing; }
        }

        public static class Iframe {
            private Processing processing = new Processing();

            public Processing getProcessing() { return processing; }
            public void setProcessing(Processing processing) { this.processing = processing; }

            public static class Processing {
                private boolean enabled = true;
                private int timeoutSeconds = 15;

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }

                public int getTimeoutSeconds() { return timeoutSeconds; }
                public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
            }
        }

        public static class Analytics {
            private Trigger trigger = new Trigger();

            public Trigger getTrigger() { return trigger; }
            public void setTrigger(Trigger trigger) { this.trigger = trigger; }

            public static class Trigger {
                private boolean enabled = true;

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
            }
        }
    }

    public static class Cookie {
        private Collection collection = new Collection();

        public Collection getCollection() { return collection; }
        public void setCollection(Collection collection) { this.collection = collection; }

        public static class Collection {
            private int phases = 5;
            private int intervalMs = 2000;
            private Deduplication deduplication = new Deduplication();
            private Incremental incremental = new Incremental();

            public int getPhases() { return phases; }
            public void setPhases(int phases) { this.phases = phases; }

            public int getIntervalMs() { return intervalMs; }
            public void setIntervalMs(int intervalMs) { this.intervalMs = intervalMs; }

            public Deduplication getDeduplication() { return deduplication; }
            public void setDeduplication(Deduplication deduplication) { this.deduplication = deduplication; }

            public Incremental getIncremental() { return incremental; }
            public void setIncremental(Incremental incremental) { this.incremental = incremental; }

            public static class Deduplication {
                private boolean enabled = true;

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
            }

            public static class Incremental {
                private boolean save = true;

                public boolean isSave() { return save; }
                public void setSave(boolean save) { this.save = save; }
            }
        }
    }

    public static class Browser {
        private Pool pool = new Pool();
        private Reuse reuse = new Reuse();
        private Memory memory = new Memory();
        private Resource resource = new Resource();

        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }

        public Reuse getReuse() { return reuse; }
        public void setReuse(Reuse reuse) { this.reuse = reuse; }

        public Memory getMemory() { return memory; }
        public void setMemory(Memory memory) { this.memory = memory; }

        public Resource getResource() { return resource; }
        public void setResource(Resource resource) { this.resource = resource; }

        public static class Pool {
            private int size = 3;

            public int getSize() { return size; }
            public void setSize(int size) { this.size = size; }
        }

        public static class Reuse {
            private boolean enabled = false;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }

        public static class Memory {
            private Cleanup cleanup = new Cleanup();

            public Cleanup getCleanup() { return cleanup; }
            public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }

            public static class Cleanup {
                private boolean enabled = true;

                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
            }
        }

        public static class Resource {
            private int timeoutMinutes = 10;

            public int getTimeoutMinutes() { return timeoutMinutes; }
            public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
        }
    }

    public static class Privacy {
        private String mode = "strict";
        private User user = new User();
        private boolean geolocationEnabled = false;
        private boolean notificationsEnabled = false;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }

        public boolean isGeolocationEnabled() { return geolocationEnabled; }
        public void setGeolocationEnabled(boolean geolocationEnabled) { this.geolocationEnabled = geolocationEnabled; }

        public boolean isNotificationsEnabled() { return notificationsEnabled; }
        public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

        public static class User {
            private Data data = new Data();

            public Data getData() { return data; }
            public void setData(Data data) { this.data = data; }

            public static class Data {
                private boolean collection = false;

                public boolean isCollection() { return collection; }
                public void setCollection(boolean collection) { this.collection = collection; }
            }
        }
    }


    public static class Hyperlinks {
        private boolean enabled = true;
        private int priorityMax = 5;
        private int navigationMax = 2;
        private int subdomainMax = 3;
        private int timeoutSeconds = 15;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPriorityMax() {
            return priorityMax;
        }

        public void setPriorityMax(int priorityMax) {
            this.priorityMax = priorityMax;
        }

        public int getNavigationMax() {
            return navigationMax;
        }

        public void setNavigationMax(int navigationMax) {
            this.navigationMax = navigationMax;
        }

        public int getSubdomainMax() {
            return subdomainMax;
        }

        public void setSubdomainMax(int subdomainMax) {
            this.subdomainMax = subdomainMax;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    private Hyperlinks hyperlinks = new Hyperlinks();
    public Hyperlinks getHyperlinks() { return hyperlinks; }

}