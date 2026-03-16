package com.warp.warp_backend.unit;

import com.warp.warp_backend.model.general.UserAgentInfo;
import com.warp.warp_backend.util.UserAgentUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UserAgentUtilTest {

  private static final String CHROME_DESKTOP =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

  private static final String FIREFOX_DESKTOP =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0";

  private static final String IPHONE_SAFARI =
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

  private static final String ANDROID_CHROME =
      "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36";

  private static final String IPAD_SAFARI =
      "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

  private UserAgentUtil userAgentUtil;

  @BeforeEach
  void setUp() throws IOException {
    userAgentUtil = new UserAgentUtil();
  }

  // --- parseUserAgent ---

  @Test
  void parseUserAgent_null_returnsUnknown() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(null);
    Assertions.assertEquals("Unknown", result.getBrowser());
    Assertions.assertEquals("Unknown", result.getDeviceType());
  }

  @Test
  void parseUserAgent_desktopChrome_returnsChromeDesktop() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(CHROME_DESKTOP);
    Assertions.assertEquals("Chrome", result.getBrowser());
    Assertions.assertEquals("Desktop", result.getDeviceType());
  }

  @Test
  void parseUserAgent_desktopFirefox_returnsFirefoxDesktop() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(FIREFOX_DESKTOP);
    Assertions.assertEquals("Firefox", result.getBrowser());
    Assertions.assertEquals("Desktop", result.getDeviceType());
  }

  @Test
  void parseUserAgent_iphone_returnsMobileSafariMobile() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(IPHONE_SAFARI);
    Assertions.assertEquals("Mobile Safari", result.getBrowser());
    Assertions.assertEquals("Mobile", result.getDeviceType());
  }

  @Test
  void parseUserAgent_androidChrome_returnsChromesMobile() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(ANDROID_CHROME);
    Assertions.assertEquals("Chrome Mobile", result.getBrowser());
    Assertions.assertEquals("Mobile", result.getDeviceType());
  }

  @Test
  void parseUserAgent_ipad_returnsMobileSafariTablet() {
    UserAgentInfo result = userAgentUtil.parseUserAgent(IPAD_SAFARI);
    Assertions.assertEquals("Mobile Safari", result.getBrowser());
    Assertions.assertEquals("Tablet", result.getDeviceType());
  }

  // --- parseReferrer ---

  @Test
  void parseReferrer_null_returnsNull() {
    Assertions.assertNull(userAgentUtil.parseReferrer(null));
  }

  @Test
  void parseReferrer_blank_returnsNull() {
    Assertions.assertNull(userAgentUtil.parseReferrer(""));
  }

  @Test
  void parseReferrer_fullUrlWithWww_stripsWww() {
    Assertions.assertEquals("google.com", userAgentUtil.parseReferrer("https://www.google.com/search?q=test"));
  }

  @Test
  void parseReferrer_fullUrlWithoutWww_returnsHost() {
    Assertions.assertEquals("github.com", userAgentUtil.parseReferrer("https://github.com/user/repo"));
  }

  @Test
  void parseReferrer_uppercaseHost_returnsLowercase() {
    Assertions.assertEquals("google.com", userAgentUtil.parseReferrer("https://WWW.GOOGLE.COM/"));
  }

  @Test
  void parseReferrer_hostOnly_returnsHost() {
    Assertions.assertEquals("google.com", userAgentUtil.parseReferrer("google.com"));
  }

  // URL shorteners

  @Test
  void parseReferrer_twitterShortlink_returnsTco() {
    Assertions.assertEquals("t.co", userAgentUtil.parseReferrer("https://t.co/AbCdEfGh1J"));
  }

  @Test
  void parseReferrer_bitly_returnsBitly() {
    Assertions.assertEquals("bit.ly", userAgentUtil.parseReferrer("https://bit.ly/3xYzAbC"));
  }

  @Test
  void parseReferrer_tinyurl_returnsTinyurl() {
    Assertions.assertEquals("tinyurl.com", userAgentUtil.parseReferrer("https://tinyurl.com/abc123"));
  }

  @Test
  void parseReferrer_shortlinkWithoutScheme_returnsHost() {
    Assertions.assertEquals("t.co", userAgentUtil.parseReferrer("t.co/AbCdEfGh1J"));
  }

  // Non-www subdomains — must NOT be stripped

  @Test
  void parseReferrer_mailSubdomain_returnsFullSubdomain() {
    Assertions.assertEquals("mail.google.com", userAgentUtil.parseReferrer("https://mail.google.com/mail/u/0/"));
  }

  @Test
  void parseReferrer_hackerNewsSubdomain_returnsFullSubdomain() {
    Assertions.assertEquals("news.ycombinator.com", userAgentUtil.parseReferrer("https://news.ycombinator.com/item?id=12345678"));
  }

  // Country-code TLDs

  @Test
  void parseReferrer_wwwCountryTld_stripsWww() {
    Assertions.assertEquals("bbc.co.uk", userAgentUtil.parseReferrer("https://www.bbc.co.uk/news/technology"));
  }

  @Test
  void parseReferrer_countryTldWithoutWww_returnsHost() {
    Assertions.assertEquals("amazon.co.jp", userAgentUtil.parseReferrer("https://amazon.co.jp/dp/B09XYZ"));
  }

  // Search engines

  @Test
  void parseReferrer_bing_returnsBing() {
    Assertions.assertEquals("bing.com", userAgentUtil.parseReferrer("https://www.bing.com/search?q=url+shortener"));
  }

  @Test
  void parseReferrer_duckduckgo_returnsDuckduckgo() {
    Assertions.assertEquals("duckduckgo.com", userAgentUtil.parseReferrer("https://duckduckgo.com/?q=url+shortener&ia=web"));
  }

  @Test
  void parseReferrer_yahooSearch_returnsSearchSubdomain() {
    Assertions.assertEquals("search.yahoo.com", userAgentUtil.parseReferrer("https://search.yahoo.com/search?p=url+shortener"));
  }

  // Social & content platforms

  @Test
  void parseReferrer_reddit_returnsReddit() {
    Assertions.assertEquals("reddit.com", userAgentUtil.parseReferrer("https://www.reddit.com/r/programming/comments/abc/title/"));
  }

  @Test
  void parseReferrer_youtube_returnsYoutube() {
    Assertions.assertEquals("youtube.com", userAgentUtil.parseReferrer("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
  }

  @Test
  void parseReferrer_linkedin_returnsLinkedin() {
    Assertions.assertEquals("linkedin.com", userAgentUtil.parseReferrer("https://www.linkedin.com/sharing/share-offsite/?url=https://example.com"));
  }

  // URL structure edge cases

  @Test
  void parseReferrer_urlWithPort_stripsPort() {
    Assertions.assertEquals("example.com", userAgentUtil.parseReferrer("https://example.com:8080/path"));
  }

  @Test
  void parseReferrer_urlWithFragment_returnsHost() {
    Assertions.assertEquals("example.com", userAgentUtil.parseReferrer("https://example.com/page#section"));
  }

  @Test
  void parseReferrer_urlWithUtmParams_returnsHost() {
    Assertions.assertEquals("example.com", userAgentUtil.parseReferrer("https://example.com/?utm_source=google&utm_medium=cpc&utm_campaign=brand"));
  }
}
