package org.yagi.motel.utils;

import org.junit.Assert;
import org.junit.Test;

public class ReplayUrlHelperTest {

    @Test
    public void testExtractUrl() {
        String hash = "231227-4a0574ed-72c1-478a-9ed4-ab5b8eb69596";
        String cnPaipuUrl = String.format("https://game.maj-soul.com/1/?paipu=%s_a205891452", hash);
        String globalPaipuUrl = String.format("https://mahjongsoul.game.yo-star.com/?paipu=%s_a925546982", hash);
        String jpPaipuUrl = String.format("https://game.mahjongsoul.com/?paipu=%s_a413984026", hash);

        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(cnPaipuUrl).get());
        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(globalPaipuUrl).get());
        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(jpPaipuUrl).get());
    }

    @Test
    public void testExtractUrlInOtherCases() {
        String hash = "231227-4a0574ed-72c1-478a-9ed4-ab5b8eb69596";

        String url = String.format("https://game.maj-soul.com/1/?paipu=%s", hash);
        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(url).get());

        url = String.format("  https://mahjongsoul.game.yo-star.com/?paipu=%s_a925546982  ", hash);
        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(url).get());

        url = String.format("http://game.mahjongsoul.com/?paipu=%s&test=89#ttttt", hash);
        Assert.assertEquals(hash, ReplayUrlHelper.extractHash(url).get());
    }
}
