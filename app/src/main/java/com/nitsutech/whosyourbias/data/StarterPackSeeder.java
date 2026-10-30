package com.nitsutech.whosyourbias.data;

import java.util.ArrayList;
import java.util.List;

public class StarterPackSeeder {

    public static List<IdolCard> getInitialIdols() {
        List<IdolCard> list = new ArrayList<>();

        // ==================== ILLIT (5th Gen / Girl Group) ====================
        String illitColor = "#00F0FF";
        String illitCol = "5th Gen, Girl Groups, HYBE / Belift";
        list.add(card("idol_illit_yunah", "Yunah", "Noh Yun-ah", "ILLIT", false, "South Korea", "2004-01-15", "Leader, Main Vocal, Dancer", illitColor, "Charismatic all-rounder and oldest member of ILLIT ('Super Real Me', 'Jellyous').", illitCol));
        list.add(card("idol_illit_minju", "Minju", "Park Min-ju", "ILLIT", false, "South Korea", "2004-05-11", "Main Vocal, Visual", illitColor, "Dreamy honey vocals & iconic tone in 'Magnetic', 'It's Me', and 'Cherish'.", illitCol));
        list.add(card("idol_illit_moka", "Moka", "Sakai Moka", "ILLIT", false, "Japan", "2004-10-08", "Lead Dancer, Vocalist, Visual", illitColor, "Master of facial expressions and stage charm from Fukuoka, Japan.", illitCol));
        list.add(card("idol_illit_wonhee", "Wonhee", "Lee Won-hee", "ILLIT", false, "South Korea", "2007-06-26", "Center, Lead Vocal", illitColor, "Bright vocal tone and magnetic center presence ('Who is your bias? It's me!').", illitCol));
        list.add(card("idol_illit_iroha", "Iroha", "Hokazono Iroha", "ILLIT", false, "Japan", "2008-02-04", "Main Dancer, Maknae", illitColor, "Hip-hop dance prodigy from Tokyo and beloved maknae of ILLIT.", illitCol));

        // ==================== NewJeans (4th Gen / Girl Group) ====================
        String njColor = "#3B82F6";
        String njCol = "4th Gen, Girl Groups, HYBE / ADOR";
        list.add(card("idol_nj_minji", "Minji", "Kim Min-ji", "NewJeans", false, "South Korea", "2004-05-07", "Vocalist, Dancer, Visual", njColor, "Classic visual and warm lower-register vocals ('Attention', 'Ditto').", njCol));
        list.add(card("idol_nj_hanni", "Hanni", "Phạm Ngọc Hân", "NewJeans", false, "Vietnam / Australia", "2004-10-06", "Main Vocal, Lead Dancer", njColor, "All-rounder vocalist, lyricist ('OMG', 'Hype Boy'), and electric stage performer.", njCol));
        list.add(card("idol_nj_danielle", "Danielle", "Danielle Marsh", "NewJeans", false, "Australia / South Korea", "2005-04-11", "Lead Vocal, Dancer", njColor, "Radiant sunshine energy and Disney-like crystal vocals.", njCol));
        list.add(card("idol_nj_haerin", "Haerin", "Kang Hae-rin", "NewJeans", false, "South Korea", "2006-05-15", "Lead Dancer, Vocalist, Visual", njColor, "Famous cat-like visual with razor-sharp footwork and groovy vocals.", njCol));
        list.add(card("idol_nj_hyein", "Hyein", "Lee Hye-in", "NewJeans", false, "South Korea", "2008-04-21", "Vocalist, Dancer, Maknae", njColor, "Charismatic R&B tone and tall fashion icon maknae.", njCol));

        // ==================== LE SSERAFIM (4th Gen / Girl Group) ====================
        String lsfColor = "#7B61FF";
        String lsfCol = "4th Gen, Girl Groups, HYBE / Source";
        list.add(card("idol_lsf_chaewon", "Chaewon", "Kim Chae-won", "LE SSERAFIM", false, "South Korea", "2000-08-01", "Leader, Main Vocal, Dancer", lsfColor, "Fearless leader with explosive stage control and clear vocals.", lsfCol));
        list.add(card("idol_lsf_sakura", "Sakura", "Miyawaki Sakura", "LE SSERAFIM", false, "Japan", "1998-03-19", "Vocalist, Visual", lsfColor, "Legendary idol veteran with unmatched charisma and dedication.", lsfCol));
        list.add(card("idol_lsf_yunjin", "Yunjin", "Huh Yun-jin", "LE SSERAFIM", false, "USA / South Korea", "2001-10-08", "Main Vocal, Songwriter", lsfColor, "Powerhouse vocalist and singer-songwriter trained in opera.", lsfCol));
        list.add(card("idol_lsf_kazuha", "Kazuha", "Nakamura Kazuha", "LE SSERAFIM", false, "Japan", "2003-08-09", "Main Dancer, Rapper, Visual", lsfColor, "Professional ballerina turned K-Pop star with graceful strength.", lsfCol));
        list.add(card("idol_lsf_eunchae", "Eunchae", "Hong Eun-chae", "LE SSERAFIM", false, "South Korea", "2006-11-10", "Lead Dancer, Vocalist, Maknae", lsfColor, "Bright smile and infectious stage energy ('Manchae').", lsfCol));

        // ==================== aespa (4th Gen / Girl Group) ====================
        String aespaColor = "#A855F7";
        String aespaCol = "4th Gen, Girl Groups, SM Entertainment";
        list.add(card("idol_aespa_karina", "Karina", "Yu Ji-min", "aespa", false, "South Korea", "2000-04-11", "Leader, Main Dancer, Rapper, Visual", aespaColor, "Futuristic AI-level visual, powerful dancer and charismatic leader ('Supernova').", aespaCol));
        list.add(card("idol_aespa_giselle", "Giselle", "Uchinaga Aeri", "aespa", false, "Japan", "2000-10-30", "Main Rapper, Vocalist", aespaColor, "Trilingual rapper with effortless cool vibes ('That's hot!').", aespaCol));
        list.add(card("idol_aespa_winter", "Winter", "Kim Min-jeong", "aespa", false, "South Korea", "2001-01-01", "Main Vocal, Lead Dancer, Visual", aespaColor, "Piercing high notes, electric guitar skills, and versatile stage aura.", aespaCol));
        list.add(card("idol_aespa_ningning", "Ningning", "Ning Yizhuo", "aespa", false, "China", "2002-10-23", "Main Vocal, Maknae", aespaColor, "Diva powerhouse vocals and fierce runway confidence.", aespaCol));

        // ==================== Stray Kids (4th Gen / Boy Group) ====================
        String skzColor = "#EF4444";
        String skzCol = "4th Gen, Boy Groups, JYP Entertainment";
        list.add(card("idol_skz_bangchan", "Bang Chan", "Christopher Bang", "Stray Kids", false, "Australia", "1997-10-03", "Leader, Producer (3RACHA), Vocal, Dancer", skzColor, "Heart and mastermind producer of Stray Kids.", skzCol));
        list.add(card("idol_skz_leeknow", "Lee Know", "Lee Min-ho", "Stray Kids", false, "South Korea", "1998-10-25", "Dance Leader, Vocalist, Visual", skzColor, "Precision main dancer with sharp wit and smooth falsetto.", skzCol));
        list.add(card("idol_skz_changbin", "Changbin", "Seo Chang-bin", "Stray Kids", false, "South Korea", "1999-08-11", "Main Rapper, Producer (3RACHA)", skzColor, "Lightning-fast thunderous rapper and hitmaker producer.", skzCol));
        list.add(card("idol_skz_hyunjin", "Hyunjin", "Hwang Hyun-jin", "Stray Kids", false, "South Korea", "2000-03-20", "Main Dancer, Rapper, Visual", skzColor, "Artistic performer with mesmerizing stage intensity.", skzCol));
        list.add(card("idol_skz_han", "Han", "Han Ji-sung", "Stray Kids", false, "South Korea", "2000-09-14", "Main Rapper, Main Vocal, Producer", skzColor, "True ace of K-Pop: freestyle rapper, rock high-note vocalist, and producer.", skzCol));
        list.add(card("idol_skz_felix", "Felix", "Lee Yong-bok", "Stray Kids", false, "Australia", "2000-09-15", "Lead Dancer, Rapper, Visual", skzColor, "Unforgettable sub-bass deep voice paired with sunshine freckles.", skzCol));
        list.add(card("idol_skz_seungmin", "Seungmin", "Kim Seung-min", "Stray Kids", false, "South Korea", "2000-09-22", "Main Vocal", skzColor, "Rock-solid live vocal stability and emotional depth.", skzCol));
        list.add(card("idol_skz_in", "I.N", "Yang Jeong-in", "Stray Kids", false, "South Korea", "2001-02-08", "Vocalist, Maknae", skzColor, "Unique vocal timbre and charismatic 'Maknae on Top'.", skzCol));

        // ==================== BTS (3rd Gen / Boy Group) ====================
        String btsColor = "#8B5CF6";
        String btsCol = "3rd Gen, Boy Groups, Legends, HYBE";
        list.add(card("idol_bts_rm", "RM", "Kim Nam-joon", "BTS", false, "South Korea", "1994-09-12", "Leader, Main Rapper, Producer", btsColor, "Poetic lyricist and visionary leader of BTS.", btsCol));
        list.add(card("idol_bts_jin", "Jin", "Kim Seok-jin", "BTS", false, "South Korea", "1992-12-04", "Vocalist, Visual", btsColor, "Silver Voice vocalist and 'Worldwide Handsome'.", btsCol));
        list.add(card("idol_bts_suga", "Suga", "Min Yoon-gi", "BTS", false, "South Korea", "1993-03-09", "Lead Rapper, Producer (Agust D)", btsColor, "Master producer and raw emotional rapper.", btsCol));
        list.add(card("idol_bts_jhope", "J-Hope", "Jung Ho-seok", "BTS", false, "South Korea", "1994-02-18", "Main Dancer, Rapper", btsColor, "World-class street dancer and stage maestro.", btsCol));
        list.add(card("idol_bts_jimin", "Jimin", "Park Ji-min", "BTS", false, "South Korea", "1995-10-13", "Main Dancer, Lead Vocal", btsColor, "Hypnotic contemporary dancer and angelic high-register tone.", btsCol));
        list.add(card("idol_bts_v", "V", "Kim Tae-hyung", "BTS", false, "South Korea", "1995-12-30", "Vocalist, Dancer, Visual", btsColor, "Soulful baritone voice and iconic expressive performer.", btsCol));
        list.add(card("idol_bts_jungkook", "Jungkook", "Jeon Jung-kook", "BTS", false, "South Korea", "1997-09-01", "Main Vocal, Lead Dancer, Center, Maknae", btsColor, "The 'Golden Maknae' dominating global pop charts ('Seven', 'Standing Next to You').", btsCol));

        // ==================== TWICE (3rd Gen / Girl Group) ====================
        String twiceColor = "#FB7185";
        String twiceCol = "3rd Gen, Girl Groups, JYP Entertainment";
        list.add(card("idol_twice_nayeon", "Nayeon", "Im Na-yeon", "TWICE", false, "South Korea", "1995-09-22", "Lead Vocal, Lead Dancer, Center", twiceColor, "Quintessential K-Pop center and solo star ('POP!', 'ABCD').", twiceCol));
        list.add(card("idol_twice_jeongyeon", "Jeongyeon", "Yoo Jeong-yeon", "TWICE", false, "South Korea", "1996-11-01", "Lead Vocal", twiceColor, "Powerful resonant vocals and warm guardian of TWICE.", twiceCol));
        list.add(card("idol_twice_momo", "Momo", "Hirai Momo", "TWICE", false, "Japan", "1996-11-09", "Main Dancer, Sub Vocal, Rapper", twiceColor, "Legendary 'Dancing Mochine' of K-Pop.", twiceCol));
        list.add(card("idol_twice_sana", "Sana", "Minatozaki Sana", "TWICE", false, "Japan", "1996-12-29", "Sub Vocal, Dancer", twiceColor, "'No Sana No Life' — unmatched charm and stage magnetism.", twiceCol));
        list.add(card("idol_twice_jihyo", "Jihyo", "Park Ji-hyo", "TWICE", false, "South Korea", "1997-02-01", "Leader, Main Vocal", twiceColor, "Ten-year trainee turned powerhouse leader who gives 200% on every stage.", twiceCol));
        list.add(card("idol_twice_mina", "Mina", "Myoui Mina", "TWICE", false, "Japan / USA", "1997-03-24", "Main Dancer, Sub Vocal", twiceColor, "Elegant black swan ballerina and silky bridge vocalist.", twiceCol));
        list.add(card("idol_twice_dahyun", "Dahyun", "Kim Da-hyun", "TWICE", false, "South Korea", "1998-05-28", "Lead Rapper, Sub Vocal", twiceColor, "Camera-finding genius, pianist, and lyricist.", twiceCol));
        list.add(card("idol_twice_chaeyoung", "Chaeyoung", "Son Chae-young", "TWICE", false, "South Korea", "1999-04-23", "Main Rapper, Sub Vocal", twiceColor, "Artistic free spirit, songwriter, and sharp rapper.", twiceCol));
        list.add(card("idol_twice_tzuyu", "Tzuyu", "Chou Tzu-yu", "TWICE", false, "Taiwan", "1999-06-14", "Lead Dancer, Sub Vocal, Visual, Maknae", twiceColor, "Statuesque visual icon and sincere maknae.", twiceCol));

        // ==================== BLACKPINK (3rd Gen / Girl Group / YG) ====================
        String bpColor = "#F43F5E";
        String bpCol = "3rd Gen, Girl Groups, YG Entertainment, Soloists, Legends";
        list.add(card("idol_bp_jisoo", "Jisoo", "Kim Ji-soo", "BLACKPINK", false, "South Korea", "1995-01-03", "Lead Vocal, Visual", bpColor, "Husky vocal charm and global actress/ambassador ('Flower').", bpCol));
        list.add(card("idol_bp_jennie", "Jennie", "Kim Jennie", "BLACKPINK", false, "South Korea", "1996-01-16", "Main Rapper, Lead Vocal", bpColor, "Trendsetting 'It Girl' with fiery rap delivery and soulful vocals ('Mantra').", bpCol));
        list.add(card("idol_bp_rose", "Rosé", "Roseanne Park", "BLACKPINK", false, "Australia / New Zealand", "1997-02-11", "Main Vocal, Lead Dancer", bpColor, "Golden indie-pop vocal color and global smash hitmaker ('APT.').", bpCol));
        list.add(card("idol_bp_lisa", "Lisa", "Lalisa Manobal", "BLACKPINK", false, "Thailand", "1997-03-27", "Main Dancer, Lead Rapper, Maknae", bpColor, "Global performance queen with unmatched stage swag ('Rockstar').", bpCol));

        // ==================== IVE (4th Gen / Girl Group) ====================
        String iveColor = "#EC4899";
        String iveCol = "4th Gen, Girl Groups";
        list.add(card("idol_ive_yujin", "An Yujin", "An Yu-jin", "IVE", false, "South Korea", "2003-09-01", "Leader, Main Dancer, Main Vocal", iveColor, "Commanding all-rounder leader and variety superstar.", iveCol));
        list.add(card("idol_ive_gaeul", "Gaeul", "Kim Ga-eul", "IVE", false, "South Korea", "2002-09-24", "Main Dancer, Rapper", iveColor, "Cool unnie with crisp dance lines and killer rap verses.", iveCol));
        list.add(card("idol_ive_rei", "Rei", "Naoi Rei", "IVE", false, "Japan", "2004-02-03", "Main Rapper, Vocalist", iveColor, "Trendy R&B hip-hop sensibility and quirky charm.", iveCol));
        list.add(card("idol_ive_wonyoung", "Wonyoung", "Jang Won-young", "IVE", false, "South Korea", "2004-08-31", "Center, Vocalist, Visual", iveColor, "Born-to-be-idol icon and creator of the 'Lucky Vicky' mindset.", iveCol));
        list.add(card("idol_ive_liz", "Liz", "Kim Ji-won", "IVE", false, "South Korea", "2004-11-21", "Main Vocal", iveColor, "Clear, soaring high notes from Jeju Island.", iveCol));
        list.add(card("idol_ive_leeseo", "Leeseo", "Lee Hyun-seo", "IVE", false, "South Korea", "2007-02-21", "Lead Dancer, Vocalist, Maknae", iveColor, "Energetic vitamin maknae of IVE.", iveCol));

        // ==================== SEVENTEEN (3rd Gen / Boy Group) ====================
        String svtColor = "#38BDF8";
        String svtCol = "3rd Gen, Boy Groups, HYBE / Pledis";
        list.add(card("idol_svt_scoups", "S.Coups", "Choi Seung-cheol", "SEVENTEEN", false, "South Korea", "1995-08-08", "General Leader, Hip-Hop Team Leader", svtColor, "Fierce general leader of the 13-member synchronization kings.", svtCol));
        list.add(card("idol_svt_jeonghan", "Jeonghan", "Yoon Jeong-han", "SEVENTEEN", false, "South Korea", "1995-10-04", "Lead Vocal, Visual", svtColor, "Clever strategist with angelic vocals.", svtCol));
        list.add(card("idol_svt_joshua", "Joshua", "Joshua Hong", "SEVENTEEN", false, "USA", "1995-12-30", "Lead Vocal, Visual", svtColor, "Gentle acoustic guitarist and sweet vocal tone from LA.", svtCol));
        list.add(card("idol_svt_hoshi", "Hoshi", "Kwon Soon-young", "SEVENTEEN", false, "South Korea", "1996-06-15", "Performance Team Leader, Main Dancer, Vocal", svtColor, "'Horanghae!' — passionate tiger choreographer and performer.", svtCol));
        list.add(card("idol_svt_wonwoo", "Wonwoo", "Jeon Won-woo", "SEVENTEEN", false, "South Korea", "1996-07-17", "Lead Rapper", svtColor, "Deep cave-like baritone rap voice and gamer spirit.", svtCol));
        list.add(card("idol_svt_woozi", "Woozi", "Lee Ji-hoon", "SEVENTEEN", false, "South Korea", "1996-11-22", "Vocal Team Leader, Main Producer", svtColor, "Genius composer behind SEVENTEEN's entire discography.", svtCol));
        list.add(card("idol_svt_mingyu", "Mingyu", "Kim Min-gyu", "SEVENTEEN", false, "South Korea", "1997-04-06", "Lead Rapper, Visual", svtColor, "Tall visual powerhouse and charismatic stage center.", svtCol));
        list.add(card("idol_svt_dk", "DK", "Lee Seok-min", "SEVENTEEN", false, "South Korea", "1997-02-18", "Main Vocal", svtColor, "Musical-theater level vocal projection and endless positivity.", svtCol));
        list.add(card("idol_svt_seungkwan", "Seungkwan", "Boo Seung-kwan", "SEVENTEEN", false, "South Korea", "1998-01-16", "Main Vocal", svtColor, "Emotional vocal powerhouse and K-Pop ambassador.", svtCol));
        list.add(card("idol_svt_vernon", "Vernon", "Hansol Vernon Chwe", "SEVENTEEN", false, "USA / South Korea", "1998-02-18", "Main Rapper", svtColor, "Effortless flow, creative lyricist, and pop-culture icon.", svtCol));
        list.add(card("idol_svt_dino", "Dino", "Lee Chan", "SEVENTEEN", false, "South Korea", "1999-02-11", "Main Dancer, Rapper, Maknae", svtColor, "'Future of K-Pop' — razor-sharp dancer and all-rounder maknae.", svtCol));
        list.add(card("idol_svt_jun", "Jun", "Wen Junhui", "SEVENTEEN", false, "China", "1996-06-10", "Lead Dancer, Sub Vocal", svtColor, "Martial arts flexibility and expressive performance member.", svtCol));
        list.add(card("idol_svt_the8", "The8", "Xu Minghao", "SEVENTEEN", false, "China", "1997-11-07", "Lead Dancer, Sub Vocal", svtColor, "B-boy master, contemporary dancer, and calm philosopher.", svtCol));

        // ==================== (G)I-DLE (4th Gen / Girl Group) ====================
        String gidleColor = "#C084FC";
        String gidleCol = "4th Gen, Girl Groups";
        list.add(card("idol_gidle_soyeon", "Soyeon", "Jeon So-yeon", "(G)I-DLE", false, "South Korea", "1998-08-26", "Leader, Main Rapper, Producer", gidleColor, "Self-producing genius leader and fierce spitfire rapper.", gidleCol));
        list.add(card("idol_gidle_miyeon", "Miyeon", "Cho Mi-yeon", "(G)I-DLE", false, "South Korea", "1997-01-31", "Main Vocal, Visual", gidleColor, "Classic royal visual with rich, soaring main vocals.", gidleCol));
        list.add(card("idol_gidle_minnie", "Minnie", "Nicha Yontararak", "(G)I-DLE", false, "Thailand", "1997-10-23", "Main Vocal, Composer", gidleColor, "Hypnotic siren vocal tone and dreamy composer.", gidleCol));
        list.add(card("idol_gidle_yuqi", "Yuqi", "Song Yuqi", "(G)I-DLE", false, "China", "1999-09-23", "Main Dancer, Sub Vocal, Rapper", gidleColor, "Distinctive husky alto voice and boundless charisma ('FREAK').", gidleCol));
        list.add(card("idol_gidle_shuhua", "Shuhua", "Yeh Shuhua", "(G)I-DLE", false, "Taiwan", "2000-01-06", "Sub Vocal, Visual, Maknae", gidleColor, "Fearless, witty visual maknae with glowing stage evolution.", gidleCol));

        // ==================== BABYMONSTER (5th Gen / Girl Group / YG) ====================
        String bmColor = "#F97316";
        String bmCol = "5th Gen, Girl Groups, YG Entertainment";
        list.add(card("idol_bm_ruka", "Ruka", "Kawai Ruka", "BABYMONSTER", false, "Japan", "2002-03-20", "Main Dancer, Main Rapper", bmColor, "Authentic hip-hop swag and seasoned dance precision.", bmCol));
        list.add(card("idol_bm_pharita", "Pharita", "Pharita Boonpakdeethaveeyod", "BABYMONSTER", false, "Thailand", "2005-08-26", "Lead Vocal, Visual", bmColor, "Disney princess elegance and crystal-clear high notes.", bmCol));
        list.add(card("idol_bm_asa", "Asa", "Enami Asa", "BABYMONSTER", false, "Japan", "2006-04-17", "Main Rapper, Lead Dancer", bmColor, "Machine-gun speed rapper and songwriter.", bmCol));
        list.add(card("idol_bm_ahyeon", "Ahyeon", "Jung Ah-yeon", "BABYMONSTER", false, "South Korea", "2007-04-11", "Center, Main Vocal, Rapper, Dancer", bmColor, "Explosive 5th-gen 'It Girl' with whistle-register high notes.", bmCol));
        list.add(card("idol_bm_rami", "Rami", "Shin Ha-ram", "BABYMONSTER", false, "South Korea", "2007-10-17", "Main Vocal", bmColor, "Soulful R&B vocal powerhouse with incredible control.", bmCol));
        list.add(card("idol_bm_rora", "Rora", "Lee Da-in", "BABYMONSTER", false, "South Korea", "2008-08-14", "Main Vocal, Visual", bmColor, "Deep emotional vocal resonance and mature elegance.", bmCol));
        list.add(card("idol_bm_chiquita", "Chiquita", "Riracha Phondechaphiphat", "BABYMONSTER", false, "Thailand", "2009-02-17", "Lead Dancer, Vocalist, Maknae", bmColor, "Born stage prodigy with instant star power.", bmCol));

        // ==================== NMIXX (4th Gen / Girl Group) ====================
        String nmixxColor = "#06B6D4";
        String nmixxCol = "4th Gen, Girl Groups, JYP Entertainment";
        list.add(card("idol_nmixx_haewon", "Haewon", "Oh Hae-won", "NMIXX", false, "South Korea", "2003-02-25", "Leader, Main Vocal", nmixxColor, "Vocal virtuoso and beloved variety comedy queen.", nmixxCol));
        list.add(card("idol_nmixx_lily", "Lily", "Lily Jin Morrow", "NMIXX", false, "Australia / South Korea", "2002-10-17", "Main Vocal", nmixxColor, "Unstoppable vocal powerhouse whose belts shake the arena.", nmixxCol));
        list.add(card("idol_nmixx_sullyoon", "Sullyoon", "Seol Yoon-a", "NMIXX", false, "South Korea", "2004-01-26", "Lead Vocal, Dancer, Visual", nmixxColor, "Doll-like top visual backed by booming live vocals.", nmixxCol));
        list.add(card("idol_nmixx_bae", "Bae", "Bae Jin-sol", "NMIXX", false, "South Korea", "2004-12-28", "Vocalist, Dancer", nmixxColor, "Rich jazzy lower-register voice and magnetic humor.", nmixxCol));
        list.add(card("idol_nmixx_jiwoo", "Jiwoo", "Kim Ji-woo", "NMIXX", false, "South Korea", "2005-04-13", "Main Rapper, Lead Dancer, Vocal", nmixxColor, "Dynamic rapper and energetic hip-hop dancer.", nmixxCol));
        list.add(card("idol_nmixx_kyujin", "Kyujin", "Jang Kyu-jin", "NMIXX", false, "South Korea", "2006-05-26", "Main Dancer, Rapper, Vocal, Maknae", nmixxColor, "Ace maknae who masters every dance, vocal, and rap part.", nmixxCol));

        // ==================== ENHYPEN (4th Gen / Boy Group) ====================
        String enhaColor = "#E11D48";
        String enhaCol = "4th Gen, Boy Groups, HYBE / Belift";
        list.add(card("idol_enha_jungwon", "Jungwon", "Yang Jung-won", "ENHYPEN", false, "South Korea", "2004-02-09", "Leader, Lead Vocal, Lead Dancer", enhaColor, "Composed leader with unique vocal color and taekwondo sharpness.", enhaCol));
        list.add(card("idol_enha_heeseung", "Heeseung", "Lee Hee-seung", "ENHYPEN", false, "South Korea", "2001-10-15", "Main Vocal, Center", enhaColor, "All-rounder ace with perfect pitch and R&B finesse.", enhaCol));
        list.add(card("idol_enha_jay", "Jay", "Park Jong-seong", "ENHYPEN", false, "USA / South Korea", "2002-04-20", "Lead Dancer, Vocalist, Guitarist", enhaColor, "Rockstar guitarist with passionate vocals and charisma.", enhaCol));
        list.add(card("idol_enha_jake", "Jake", "Sim Jae-yun", "ENHYPEN", false, "Australia / South Korea", "2002-11-15", "Vocalist, Dancer", enhaColor, "Warm Aussie charm and infectious groove ('Bite Me').", enhaCol));
        list.add(card("idol_enha_sunghoon", "Sunghoon", "Park Sung-hoon", "ENHYPEN", false, "South Korea", "2002-12-08", "Lead Dancer, Vocalist, Visual", enhaColor, "Former national figure skater and 'Ice Prince' visual.", enhaCol));
        list.add(card("idol_enha_sunoo", "Sunoo", "Kim Sun-oo", "ENHYPEN", false, "South Korea", "2003-06-24", "Vocalist", enhaColor, "Expressive duality king shifting from bright sunshine to dark concept.", enhaCol));
        list.add(card("idol_enha_niki", "Ni-ki", "Nishimura Riki", "ENHYPEN", false, "Japan", "2005-12-09", "Main Dancer, Maknae", enhaColor, "Generational dance prodigy with unmatched body control.", enhaCol));

        // ==================== TXT (4th Gen / Boy Group) ====================
        String txtColor = "#10B981";
        String txtCol = "4th Gen, Boy Groups, HYBE / BigHit";
        list.add(card("idol_txt_soobin", "Soobin", "Choi Soo-bin", "TXT", false, "South Korea", "2000-12-05", "Leader, Vocalist, Dancer", txtColor, "Beloved dimpled leader with dreamy falsetto.", txtCol));
        list.add(card("idol_txt_yeonjun", "Yeonjun", "Choi Yeon-jun", "TXT", false, "South Korea", "1999-09-13", "Main Dancer, Rapper, Vocalist", txtColor, "Legendary '4th Gen It Boy' and solo performer ('GGUM').", txtCol));
        list.add(card("idol_txt_beomgyu", "Beomgyu", "Choi Beom-gyu", "TXT", false, "South Korea", "2001-03-13", "Vocalist, Dancer, Center, Visual", txtColor, "Heart-fluttering visual, guitarist, and mood-maker.", txtCol));
        list.add(card("idol_txt_taehyun", "Taehyun", "Kang Tae-hyun", "TXT", false, "South Korea", "2002-02-05", "Main Vocal", txtColor, "Rock-flavored high notes and sharp intellect.", txtCol));
        list.add(card("idol_txt_hueningkai", "HueningKai", "Kai Kamal Huening", "TXT", false, "USA / South Korea", "2002-08-14", "Multi-instrumentalist, Vocalist, Maknae", txtColor, "Musical prodigy playing piano, drums, and guitar effortlessly.", txtCol));

        // ==================== ATEEZ (4th Gen / Boy Group) ====================
        String ateezColor = "#F59E0B";
        String ateezCol = "4th Gen, Boy Groups";
        list.add(card("idol_ateez_hongjoong", "Hongjoong", "Kim Hong-joong", "ATEEZ", false, "South Korea", "1998-11-07", "Captain, Main Rapper, Composer", ateezColor, "Charismatic Captain of ATEEZ with iconic laugh and fashion style.", ateezCol));
        list.add(card("idol_ateez_seonghwa", "Seonghwa", "Park Seong-hwa", "ATEEZ", false, "South Korea", "1998-04-03", "Vocalist, Rapper, Visual", ateezColor, "Theatrical stage mastery and striking visual presence.", ateezCol));
        list.add(card("idol_ateez_yunho", "Yunho", "Jeong Yun-ho", "ATEEZ", false, "South Korea", "1999-03-23", "Main Dancer, Vocalist", ateezColor, "Golden retriever energy paired with effortless dance precision.", ateezCol));
        list.add(card("idol_ateez_yeosang", "Yeosang", "Kang Yeo-sang", "ATEEZ", false, "South Korea", "1999-06-15", "Lead Dancer, Vocalist, Visual", ateezColor, "Sculpted visual with an unexpectedly deep bass voice.", ateezCol));
        list.add(card("idol_ateez_san", "San", "Choi San", "ATEEZ", false, "South Korea", "1999-07-10", "Main Dancer, Lead Vocal", ateezColor, "Performance monster who dances like every stage is his last.", ateezCol));
        list.add(card("idol_ateez_mingi", "Mingi", "Song Min-gi", "ATEEZ", false, "South Korea", "1999-08-09", "Main Rapper, Main Dancer", ateezColor, "'Fix On!' — booming low-tone rapper and hype king.", ateezCol));
        list.add(card("idol_ateez_wooyoung", "Wooyoung", "Jung Woo-young", "ATEEZ", false, "South Korea", "1999-11-26", "Main Dancer, Vocalist", ateezColor, "Sensual choreography specialist and social butterfly.", ateezCol));
        list.add(card("idol_ateez_jongho", "Jongho", "Choi Jong-ho", "ATEEZ", false, "South Korea", "2000-10-12", "Main Vocal, Maknae", ateezColor, "Splits apples in half with his bare hands while belting 4-octave high notes.", ateezCol));

        // ==================== BOYNEXTDOOR (5th Gen / Boy Group) ====================
        String bndColor = "#14B8A6";
        String bndCol = "5th Gen, Boy Groups, HYBE / KOZ";
        list.add(card("idol_bnd_sungho", "Sungho", "Park Sung-ho", "BOYNEXTDOOR", false, "South Korea", "2003-09-04", "Main Vocal", bndColor, "Crisp, refreshing rock-pop vocals and broad shoulders.", bndCol));
        list.add(card("idol_bnd_riwoo", "Riwoo", "Lee Sang-hyeok", "BOYNEXTDOOR", false, "South Korea", "2003-10-22", "Main Dancer, Vocalist", bndColor, "Groovy dance machine who makes the hardest choreography look easy.", bndCol));
        list.add(card("idol_bnd_jaehyun", "Myung Jaehyun", "Myung Jae-hyun", "BOYNEXTDOOR", false, "South Korea", "2003-12-04", "Leader, Main Rapper, Producer", bndColor, "Expressive, big-hearted leader and creative songwriter.", bndCol));
        list.add(card("idol_bnd_taesan", "Taesan", "Han Dong-min", "BOYNEXTDOOR", false, "South Korea", "2004-08-10", "Rapper, Vocalist, Producer, Visual", bndColor, "Cool indie-rock sensibility and chief songwriter of BND.", bndCol));
        list.add(card("idol_bnd_leehan", "Leehan", "Kim Dong-hyun", "BOYNEXTDOOR", false, "South Korea", "2004-10-20", "Vocalist, Visual", bndColor, "Princely visual and passionate aquarium/fish enthusiast.", bndCol));
        list.add(card("idol_bnd_woonhak", "Woonhak", "Kim Woon-hak", "BOYNEXTDOOR", false, "South Korea", "2006-11-29", "Vocalist, Rapper, Maknae", bndColor, "Lovable giant maknae bursting with stage charisma.", bndCol));

        // ==================== CORTIS (5th Gen / Boy Group) ====================
        String cortisColor = "#6366F1";
        String cortisCol = "5th Gen, Boy Groups, HYBE / BigHit";
        list.add(card("idol_cortis_martin", "Martin", "Martin", "CORTIS", false, "Canada / South Korea", "2006-03-15", "Producer, Vocalist, Rapper", cortisColor, "5th Gen creative producer & performer from BigHit's new boy group CORTIS.", cortisCol));
        list.add(card("idol_cortis_james", "James", "Chao Yu-fan (James)", "CORTIS", false, "Thailand / Hong Kong", "2005-10-14", "Main Dancer, Choreographer", cortisColor, "Dynamic dancer and choreographer who co-created hits before debuting in CORTIS.", cortisCol));
        list.add(card("idol_cortis_juhoon", "Juhoon", "Kim Ju-hoon", "CORTIS", false, "South Korea", "2008-01-03", "Vocalist, Visual", cortisColor, "Fresh 5th Gen visual and expressive vocalist of CORTIS.", cortisCol));
        list.add(card("idol_cortis_seonghyeon", "Seonghyeon", "Uhm Seong-hyeon", "CORTIS", false, "South Korea", "2009-01-13", "Vocalist, Dancer", cortisColor, "Rising 5th Gen all-rounder with sleek performance style.", cortisCol));
        list.add(card("idol_cortis_keonho", "Keonho", "Ahn Keon-ho", "CORTIS", false, "South Korea", "2009-02-14", "Vocalist, Maknae", cortisColor, "Charismatic youngest member of CORTIS.", cortisCol));

        // ==================== RESCENE (5th Gen / Girl Group) ====================
        String resceneColor = "#F472B6";
        String resceneCol = "5th Gen, Girl Groups, The Muze";
        list.add(card("idol_rescene_woni", "Woni", "Jeong Won-i", "RESCENE", false, "South Korea", "2004-05-25", "Leader, Vocalist, Dancer", resceneColor, "Graceful leader of RESCENE ('LOVE ATTACK', 'Pinball', 'UhUh').", resceneCol));
        list.add(card("idol_rescene_liv", "Liv", "Jin Kyung-eun", "RESCENE", false, "South Korea", "2006-10-11", "Main Vocal, Dancer", resceneColor, "Dreamy main vocalist driving RESCENE's signature city-pop & synth melodies.", resceneCol));
        list.add(card("idol_rescene_minami", "Minami", "Ito Minami", "RESCENE", false, "Japan", "2006-11-29", "Vocalist, Rapper, Dancer", resceneColor, "All-rounder from Tokyo with bright vocals and sharp choreography.", resceneCol));
        list.add(card("idol_rescene_may", "May", "Lee Ye-bin", "RESCENE", false, "South Korea", "2008-08-19", "Vocalist, Visual", resceneColor, "Refreshing visual and sweet vocal tone in 'LOVE ATTACK'.", resceneCol));
        list.add(card("idol_rescene_zena", "Zena", "Kim Ga-young", "RESCENE", false, "South Korea", "2008-11-27", "Vocalist, Rapper, Visual, Maknae", resceneColor, "Charismatic cat-eyed visual maknae of RESCENE.", resceneCol));

        // ==================== KATSEYE (HYBE x Geffen / Global Girl Group) ====================
        String katseyeColor = "#D946EF";
        String katseyeCol = "5th Gen, Girl Groups, HYBE";
        list.add(card("idol_katseye_sophia", "Sophia", "Sophia Laforteza", "KATSEYE", false, "Philippines", "2002-12-31", "Leader, Main Vocal", katseyeColor, "Commanding leader and vocal powerhouse of KATSEYE ('Touch', 'Debut').", katseyeCol));
        list.add(card("idol_katseye_manon", "Manon", "Meret Manon Bannerman", "KATSEYE", false, "Switzerland", "2002-06-26", "Vocalist, Visual", katseyeColor, "Captivating star quality and silky lower-register tone.", katseyeCol));
        list.add(card("idol_katseye_daniela", "Daniela", "Daniela Avanzini", "KATSEYE", false, "USA", "2004-07-01", "Main Dancer, Vocalist", katseyeColor, "Champion ballroom & Latin dancer with fiery stage presence.", katseyeCol));
        list.add(card("idol_katseye_lara", "Lara", "Lara Rajagopalan", "KATSEYE", false, "USA / India", "2005-11-03", "Main Vocal", katseyeColor, "Soulful R&B vocal runs and fearless confidence.", katseyeCol));
        list.add(card("idol_katseye_megan", "Megan", "Megan Skiendiel", "KATSEYE", false, "USA", "2006-02-10", "Lead Dancer, Vocalist", katseyeColor, "Precision dancer from Hawaii with electric stage charisma.", katseyeCol));
        list.add(card("idol_katseye_yoonchae", "Yoonchae", "Jeong Yoon-chae", "KATSEYE", false, "South Korea", "2007-12-06", "Vocalist, Dancer, Maknae", katseyeColor, "Polished all-rounder maknae shining in 'Touch'.", katseyeCol));

        // ==================== TWS (HYBE / Pledis 5th Gen Boy Group) ====================
        String twsColor = "#38BDF8";
        String twsCol = "5th Gen, Boy Groups, HYBE / Pledis";
        list.add(card("idol_tws_shinyu", "Shinyu", "Shin Jung-hwan", "TWS", false, "South Korea", "2003-11-07", "Leader, Main Rapper, Visual", twsColor, "Charismatic tall leader of TWS ('plot twist').", twsCol));
        list.add(card("idol_tws_dohoon", "Dohoon", "Kim Do-hoon", "TWS", false, "South Korea", "2005-01-30", "All-Rounder, Center, Vocal, Rapper", twsColor, "Trendy 5th-gen ace and center of TWS.", twsCol));
        list.add(card("idol_tws_youngjae", "Youngjae", "Choi Young-jae", "TWS", false, "South Korea", "2005-05-31", "Main Vocal", twsColor, "Crystal-clear main vocalist of TWS.", twsCol));
        list.add(card("idol_tws_hanjin", "Hanjin", "Han Zhen", "TWS", false, "China", "2006-01-05", "Vocalist, Visual", twsColor, "Prince-like visual and warm personality.", twsCol));
        list.add(card("idol_tws_jihoon", "Jihoon", "Han Ji-hoon", "TWS", false, "South Korea", "2006-03-28", "Main Dancer, Vocalist", twsColor, "Passionate main dancer trained across top academies.", twsCol));
        list.add(card("idol_tws_kyungmin", "Kyungmin", "Lee Kyung-min", "TWS", false, "South Korea", "2007-10-02", "Lead Vocal, Maknae", twsColor, "Bright vocal tone and energetic maknae of TWS.", twsCol));

        // ==================== &TEAM (HYBE Labels Japan) ====================
        String andteamColor = "#818CF8";
        String andteamCol = "4th Gen, Boy Groups, HYBE";
        list.add(card("idol_andteam_k", "K", "Koga Yudai", "&TEAM", false, "Japan", "1997-10-21", "Main Dancer, Lead Vocal, Center", andteamColor, "Charismatic performance leader and choreographer ('Firework', 'Deer Hunter').", andteamCol));
        list.add(card("idol_andteam_ej", "EJ", "Byun Eui-joo", "&TEAM", false, "South Korea", "2002-09-07", "Leader, Vocalist, Dancer", andteamColor, "Gentle yet steadfast leader of &TEAM.", andteamCol));
        list.add(card("idol_andteam_nicholas", "Nicholas", "Wang Yixiang", "&TEAM", false, "Taiwan", "2002-07-09", "Main Rapper, Main Dancer, Vocal", andteamColor, "Multilingual b-boy, rapper, and fashion standout.", andteamCol));
        list.add(card("idol_andteam_harua", "Harua", "Shigeta Harua", "&TEAM", false, "Japan", "2005-05-01", "Vocalist, Visual", andteamColor, "Beloved visual center with sweet vocal tone.", andteamCol));
        list.add(card("idol_andteam_maki", "Maki", "Riki Maus", "&TEAM", false, "Japan / Germany", "2006-02-17", "Main Vocal, Maknae", andteamColor, "Powerhouse bilingual vocalist and maknae of &TEAM.", andteamCol));

        // ==================== fromis_9 (HYBE / Pledis) ====================
        String fromisColor = "#F43F5E";
        String fromisCol = "4th Gen, Girl Groups, HYBE / Pledis";
        list.add(card("idol_fromis_hayoung", "Hayoung", "Song Ha-young", "fromis_9", false, "South Korea", "1997-09-29", "Main Dancer, Main Vocal", fromisColor, "Whistle-note vocalist and hip-hop dance ace ('Supersonic').", fromisCol));
        list.add(card("idol_fromis_jiwon", "Jiwon", "Park Ji-won", "fromis_9", false, "South Korea", "1998-03-20", "Main Vocal", fromisColor, "'Megan' — powerhouse main vocalist and comedy queen.", fromisCol));
        list.add(card("idol_fromis_chaeyoung", "Lee Chaeyoung", "Lee Chae-young", "fromis_9", false, "South Korea", "2000-05-14", "Main Dancer, Lead Rapper", fromisColor, "Charismatic performer and variety star.", fromisCol));
        list.add(card("idol_fromis_nagyung", "Nagyung", "Lee Na-gyung", "fromis_9", false, "South Korea", "2000-06-01", "Lead Dancer, Vocalist, Visual", fromisColor, "Stunning visual and gamer icon.", fromisCol));
        list.add(card("idol_fromis_jiheon", "Jiheon", "Baek Ji-heon", "fromis_9", false, "South Korea", "2003-04-17", "Vocalist, Maknae", fromisColor, "Eye-smile queen and beloved maknae of fromis_9.", fromisCol));

        // ==================== BIGBANG & Legends (YG Entertainment) ====================
        String bbColor = "#FFD166";
        String bbCol = "2nd Gen, Boy Groups, YG Entertainment, Soloists, Legends";
        list.add(card("idol_bb_gd", "G-Dragon", "Kwon Ji-yong", "BIGBANG", false, "South Korea", "1988-08-18", "Leader, Main Rapper, Producer, Soloist", bbColor, "'King of K-Pop' — cultural icon, fashion pioneer, and hitmaker ('POWER', 'Crooked').", bbCol));
        list.add(card("idol_bb_taeyang", "Taeyang", "Dong Young-bae", "BIGBANG", false, "South Korea", "1988-05-18", "Main Vocal, Main Dancer, Soloist", bbColor, "Legendary R&B vocalist ('Eyes, Nose, Lips', 'VIBE') and groove master.", bbCol + ", THEBLACKLABEL"));
        list.add(card("idol_bb_top", "T.O.P", "Choi Seung-hyun", "BIGBANG", false, "South Korea", "1987-11-04", "Lead Rapper, Visual, Actor", bbColor, "Iconic bass-baritone charismatic rapper ('DOOM DADA', 'Fantastic Baby').", bbCol));
        list.add(card("idol_bb_daesung", "Daesung", "Kang Dae-sung", "BIGBANG", false, "South Korea", "1989-04-26", "Main Vocal, Soloist", bbColor, "Soulful rock-ballad powerhouse and beloved entertainer.", bbCol));

        // ==================== Iconic Soloists (Yena, IU, EVAN, Somi, Taeyeon) ====================
        list.add(card("idol_solo_yena", "YENA", "Choi Ye-na", "Soloist", true, "South Korea", "1999-09-29", "Soloist, Main Vocal, Rapper, Dancer", "#EC4899", "Pop-rock & bubbly soloist queen ('SMILEY', 'NEMONEMO', 'Good Morning').", "Soloists, 4th Gen, Girl Groups"));
        list.add(card("idol_solo_evan", "EVAN", "Evan", "Soloist", true, "South Korea / Global", "2001-10-15", "Soloist, Vocalist, Producer", "#00F0FF", "Smooth R&B and modern pop vocalist & performer.", "Soloists, HYBE"));
        list.add(card("idol_solo_somi", "Jeon Somi", "Ennik Somi Douma", "Soloist", true, "Canada / South Korea", "2001-03-09", "Soloist, Vocalist, Dancer", "#F43F5E", "THEBLACKLABEL solo superstar ('Fast Forward', 'DUMB DUMB', 'XOXO').", "Soloists, THEBLACKLABEL"));
        list.add(card("idol_solo_taeyeon", "Taeyeon", "Kim Tae-yeon", "Girls' Generation", false, "South Korea", "1989-03-09", "Leader, Main Vocal, Soloist", "#C084FC", "SM Entertainment's legendary vocal queen ('INVU', 'To. X').", "SM Entertainment, Soloists, Legends, Girl Groups"));
        list.add(card("idol_solo_iu", "IU", "Lee Ji-eun", "Soloist", true, "South Korea", "1993-05-16", "Soloist, Singer-Songwriter, Actress", "#A78BFA", "Korea's 'Nation's Sweetheart' and acclaimed singer-songwriter ('Love wins all').", "Soloists, Legends"));

        // ==================== KiiiKiii (Starship 5th Gen Girl Group) ====================
        String kiiikiiiColor = "#10B981";
        String kiiikiiiCol = "5th Gen, Girl Groups, Starship";
        list.add(card("idol_kiiikiii_jiyu", "Jiyu", "Seo Ji-yu", "KiiiKiii", false, "South Korea", "2006-05-14", "Leader, Vocalist, Rapper, Dancer", kiiikiiiColor, "Charismatic short-haired leader of KiiiKiii ('I DO ME').", kiiikiiiCol));
        list.add(card("idol_kiiikiii_leesol", "Leesol", "Lee Su-min", "KiiiKiii", false, "South Korea", "2005-09-18", "Main Rapper, Vocalist", kiiikiiiColor, "Chic rapper and eldest unnie of KiiiKiii with distinct low tone.", kiiikiiiCol));
        list.add(card("idol_kiiikiii_sui", "Sui", "Lee Su-bin", "KiiiKiii", false, "South Korea", "2006-04-10", "Lead Vocal, Visual", kiiikiiiColor, "Warm indie-pop vocal color and radiant smile in 'I DO ME'.", kiiikiiiCol));
        list.add(card("idol_kiiikiii_haum", "Haum", "Kwak Ha-eum", "KiiiKiii", false, "South Korea", "2006-11-14", "Lead Dancer, Vocalist, Visual", kiiikiiiColor, "Dreamy visual and expressive dancer of KiiiKiii.", kiiikiiiCol));
        list.add(card("idol_kiiikiii_kya", "Kya", "Park Ji-woo", "KiiiKiii", false, "South Korea", "2010-12-18", "Vocalist, Maknae", kiiikiiiColor, "Fearless powerhouse maknae of KiiiKiii.", kiiikiiiCol));

        // ==================== MEOVV & izna (THEBLACKLABEL) ====================
        String meovvColor = "#F59E0B";
        String tblCol = "5th Gen, Girl Groups, THEBLACKLABEL";
        list.add(card("idol_meovv_sooin", "Sooin", "Kim Soo-in", "MEOVV", false, "South Korea", "2005-04-12", "Main Dancer, Vocalist", meovvColor, "Fierce dance performance ace of THEBLACKLABEL's MEOVV ('MEOW', 'TOXIC').", tblCol));
        list.add(card("idol_meovv_gawon", "Gawon", "Lee Ga-won (Chloe Lee)", "MEOVV", false, "USA / South Korea", "2005-04-27", "Main Vocal, Rapper", meovvColor, "Runway model proportions with rich, velvety R&B vocals.", tblCol));
        list.add(card("idol_meovv_anna", "Anna", "Tanaka Anna", "MEOVV", false, "Japan", "2005-11-17", "Vocalist, Visual", meovvColor, "Magazine cover visual icon from Toyama, Japan.", tblCol));
        list.add(card("idol_meovv_narin", "Narin", "Na Rin", "MEOVV", false, "South Korea", "2007-08-15", "Main Rapper, Vocalist", meovvColor, "Magnetic all-rounder tearing up the stage in 'BODY' and 'MEOW'.", tblCol));
        list.add(card("idol_meovv_ella", "Ella", "Ella Gross", "MEOVV", false, "USA / South Korea", "2008-12-01", "Vocalist, Visual, Maknae", meovvColor, "Global star and captivating maknae of MEOVV.", tblCol));

        list.add(card("idol_izna_jeemin", "Jeemin", "Bang Jee-min", "izna", false, "South Korea", "2005-05-08", "Center, Lead Dancer, Vocal", "#FB7185", "Towering center of THEBLACKLABEL-produced group izna ('IZNA').", tblCol));
        list.add(card("idol_izna_koko", "Koko", "Narai Koko", "izna", false, "Japan", "2006-11-14", "Main Dancer, Main Rapper", "#FB7185", "Swaggy hip-hop dancer and rapid-fire rapper.", tblCol));
        list.add(card("idol_izna_saebi", "Saebi", "Jeong Sae-bi", "izna", false, "South Korea", "2008-01-22", "Vocalist, Visual, Maknae", "#FB7185", "Born star maknae with sparkling stage presence.", tblCol));

        // ==================== JYP Entertainment: ITZY, DAY6, Xdinary Heroes, KickFlip ====================
        String itzyColor = "#EC4899";
        String jypGirlCol = "4th Gen, Girl Groups, JYP Entertainment";
        list.add(card("idol_itzy_yeji", "Yeji", "Hwang Ye-ji", "ITZY", false, "South Korea", "2000-05-26", "Leader, Main Dancer, Lead Vocal", itzyColor, "JYP's 'Secret Weapon' leader with unstoppable charisma ('WANNABE', 'GOLD').", jypGirlCol));
        list.add(card("idol_itzy_lia", "Lia", "Choi Ji-su", "ITZY", false, "South Korea", "2000-07-21", "Main Vocal", itzyColor, "Honey-toned R&B main vocalist and heart of ITZY.", jypGirlCol));
        list.add(card("idol_itzy_ryujin", "Ryujin", "Shin Ryu-jin", "ITZY", false, "South Korea", "2001-04-17", "Main Rapper, Lead Dancer, Center", itzyColor, "Iconic shoulder-dance center with unmatched girl-crush aura.", jypGirlCol));
        list.add(card("idol_itzy_chaeryeong", "Chaeryeong", "Lee Chae-ryeong", "ITZY", false, "South Korea", "2001-06-05", "Main Dancer, Vocalist", itzyColor, "Feather-light yet razor-sharp main dancer and variety gem.", jypGirlCol));
        list.add(card("idol_itzy_yuna", "Yuna", "Shin Yu-na", "ITZY", false, "South Korea", "2003-12-09", "Lead Rapper, Lead Dancer, Visual, Maknae", itzyColor, "'Queen Yuna' — radiant visual maknae and stage vitamin.", jypGirlCol));

        String day6Color = "#06D6A0";
        String jypBandCol = "3rd Gen, Boy Groups, JYP Entertainment";
        list.add(card("idol_day6_sungjin", "Sungjin", "Park Sung-jin", "DAY6", false, "South Korea", "1993-01-16", "Leader, Main Vocal, Rhythm Guitar", day6Color, "Raspy rock-soul vocal leader of nation's band DAY6 ('You Were Beautiful').", jypBandCol));
        list.add(card("idol_day6_youngk", "Young K", "Kang Young-hyun", "DAY6", false, "South Korea / Canada", "1993-12-19", "Bassist, Main Vocal, Rapper, Lyricist", day6Color, "Prolific songwriter, bassist, and vocal powerhouse.", jypBandCol));
        list.add(card("idol_day6_wonpil", "Wonpil", "Kim Won-pil", "DAY6", false, "South Korea", "1994-04-28", "Synthesizer, Keyboard, Main Vocal", day6Color, "Emotional voice behind DAY6's soaring choruses.", jypBandCol));
        list.add(card("idol_day6_dowoon", "Dowoon", "Yoon Do-woon", "DAY6", false, "South Korea", "1995-08-25", "Drummer, Vocalist, Maknae", day6Color, "Beloved drummer maknae driving DAY6's rhythm.", jypBandCol));

        String xhColor = "#EF4444";
        String xhCol = "4th Gen, Boy Groups, JYP Entertainment";
        list.add(card("idol_xh_jooyeon", "Jooyeon", "Lee Joo-yeon", "Xdinary Heroes", false, "South Korea", "2002-09-12", "Bassist, Main Vocal, Visual, Maknae", xhColor, "Explosive rock high notes and visual bassist of JYP's Xdinary Heroes.", xhCol));
        list.add(card("idol_xh_gaon", "Gaon", "Kwak Ji-seok", "Xdinary Heroes", false, "South Korea", "2002-01-14", "Rhythm Guitarist, Rapper, Vocal", xhColor, "Energetic rockstar guitarist and frontman charisma.", xhCol));

        String kfColor = "#3B82F6";
        String kfCol = "5th Gen, Boy Groups, JYP Entertainment";
        list.add(card("idol_kf_kyehoon", "Kyehoon", "Lee Kye-hoon", "KickFlip", false, "South Korea", "2004-09-16", "Leader, Rapper, Dancer", kfColor, "Long-awaited JYP trainee legend and leader of 5th Gen boy group KickFlip.", kfCol));
        list.add(card("idol_kf_amaru", "Amaru", "Mitsuyuki Amaru", "KickFlip", false, "Japan", "2005-10-21", "Main Vocal, Composer", kfColor, "Soulful vocalist and songwriter of KickFlip.", kfCol));
        list.add(card("idol_kf_keiju", "Keiju", "Okamoto Keiju", "KickFlip", false, "Japan", "2006-10-04", "Main Dancer, Vocalist", kfColor, "Groovy dance prodigy of JYP's KickFlip.", kfCol));

        // ==================== SM Entertainment: Red Velvet, RIIZE, NCT, Hearts2Hearts, EXO, SHINee ====================
        String rvColor = "#F43F5E";
        String smGirlCol = "3rd Gen, Girl Groups, SM Entertainment";
        list.add(card("idol_rv_irene", "Irene", "Bae Joo-hyun", "Red Velvet", false, "South Korea", "1991-03-29", "Leader, Main Rapper, Lead Dancer, Visual", rvColor, "'Original Visual' of 3rd Gen K-Pop ('Psycho', 'Cosmic').", smGirlCol));
        list.add(card("idol_rv_seulgi", "Seulgi", "Kang Seul-gi", "Red Velvet", false, "South Korea", "1994-02-10", "Main Dancer, Lead Vocal", rvColor, "Textbook idol ace praised across the entire industry ('28 Reasons').", smGirlCol));
        list.add(card("idol_rv_wendy", "Wendy", "Son Seung-wan", "Red Velvet", false, "South Korea / Canada", "1994-02-21", "Main Vocal", rvColor, "Generational vocal titan with heavenly high notes and ad-libs.", smGirlCol));
        list.add(card("idol_rv_joy", "Joy", "Park Soo-young", "Red Velvet", false, "South Korea", "1996-09-03", "Lead Rapper, Sub Vocal", rvColor, "Refreshing vitamin voice and drama star ('Hello').", smGirlCol));
        list.add(card("idol_rv_yeri", "Yeri", "Kim Ye-rim", "Red Velvet", false, "South Korea", "1999-03-05", "Sub Vocal, Sub Rapper, Maknae", rvColor, "Charming social butterfly and maknae of Red Velvet.", smGirlCol));

        String h2hColor = "#EC4899";
        String sm5GirlCol = "5th Gen, Girl Groups, SM Entertainment";
        list.add(card("idol_h2h_jiwoo", "Jiwoo (H2H)", "Choi Ji-woo", "Hearts2Hearts", false, "South Korea", "2006-09-07", "Leader, Dancer, Visual", h2hColor, "Tall ballerina leader of SM's 5th Gen girl group Hearts2Hearts ('The Chase').", sm5GirlCol));
        list.add(card("idol_h2h_carmen", "Carmen", "Nyoman Ayu Carmenita", "Hearts2Hearts", false, "Indonesia", "2006-03-28", "Main Vocal", h2hColor, "Clear, bright main vocalist and SM's first Indonesian idol.", sm5GirlCol));
        list.add(card("idol_h2h_ian", "Ian", "Jeong Lee-an", "Hearts2Hearts", false, "South Korea", "2009-10-09", "Center, Visual, Dancer", h2hColor, "Striking visual center of Hearts2Hearts.", sm5GirlCol));

        String riizeColor = "#FB923C";
        String sm5BoyCol = "5th Gen, Boy Groups, SM Entertainment";
        list.add(card("idol_riize_shotaro", "Shotaro", "Osaki Shotaro", "RIIZE", false, "Japan", "2000-11-25", "Main Dancer", riizeColor, "World-class street dancer with infectious smile ('Get A Guitar', 'Boom Boom Bass').", sm5BoyCol));
        list.add(card("idol_riize_eunseok", "Eunseok", "Song Eun-seok", "RIIZE", false, "South Korea", "2001-03-19", "Vocalist, Visual", riizeColor, "'Black Shadow' — dry humor king and classic SM visual.", sm5BoyCol));
        list.add(card("idol_riize_sungchan", "Sungchan", "Jung Sung-chan", "RIIZE", false, "South Korea", "2001-09-13", "Rapper, Visual", riizeColor, "Tall athletic visual and charismatic rapper of RIIZE.", sm5BoyCol));
        list.add(card("idol_riize_wonbin", "Wonbin", "Park Won-bin", "RIIZE", false, "South Korea", "2002-03-02", "Center, Lead Vocal, Main Dancer, Visual", riizeColor, "5th Gen iconic center, guitarist, and explosive performer.", sm5BoyCol));
        list.add(card("idol_riize_sohee", "Sohee", "Lee So-hee", "RIIZE", false, "South Korea", "2003-11-21", "Main Vocal", riizeColor, "Effortless sky-high vocal range and emotional pop tone.", sm5BoyCol));
        list.add(card("idol_riize_anton", "Anton", "Anton Chanyoung Lee", "RIIZE", false, "USA / South Korea", "2004-03-21", "Vocalist, Cellist, Producer, Maknae", riizeColor, "Soft-spoken cellist, swimmer, and producer maknae of RIIZE.", sm5BoyCol));

        String nctColor = "#84CC16";
        String smBoyCol = "3rd Gen, 4th Gen, Boy Groups, SM Entertainment";
        list.add(card("idol_nct_mark", "Mark", "Mark Lee", "NCT", false, "Canada", "1999-08-02", "Main Rapper, Main Dancer, Leader", nctColor, "Hardest-working ace of K-Pop across NCT 127, NCT DREAM, and solo.", smBoyCol));
        list.add(card("idol_nct_taeyong", "Taeyong", "Lee Tae-yong", "NCT", false, "South Korea", "1995-07-01", "Leader, Main Rapper, Main Dancer, Center", nctColor, "Visionary neo-culture center and choreographer ('SHALALA', 'TAP').", smBoyCol));
        list.add(card("idol_nct_jaehyun", "Jaehyun", "Jeong Yun-o", "NCT", false, "South Korea", "1997-02-14", "Main Vocal, Lead Dancer, Visual", nctColor, "Velvety R&B baritone and timeless leading-man visual ('Smoke').", smBoyCol));
        list.add(card("idol_nct_haechan", "Haechan", "Lee Dong-hyuck", "NCT", false, "South Korea", "2000-06-06", "Main Vocal, Lead Dancer", nctColor, "'FullSun' — one-of-a-kind honey vocal signature of NCT.", smBoyCol));
        list.add(card("idol_nct_jaemin", "Jaemin", "Na Jae-min", "NCT", false, "South Korea", "2000-08-13", "Lead Dancer, Rapper, Center, Visual", nctColor, "Beloved 'Nana' center of NCT DREAM.", smBoyCol));
        list.add(card("idol_nct_ten", "Ten", "Chittaphon Leechaiyapornkul", "NCT", false, "Thailand", "1996-02-27", "Main Dancer, Main Vocal", nctColor, "Artistic performance genius of WayV & NCT ('Nightwalker').", smBoyCol));
        list.add(card("idol_nct_sion", "Sion", "Oh Si-on", "NCT WISH", false, "South Korea", "2002-05-11", "Leader, Lead Dancer, Vocal, Visual", nctColor, "Bright leader of SM's 5th Gen unit NCT WISH.", "5th Gen, Boy Groups, SM Entertainment"));

        String exoShineeCol = "3rd Gen, 2nd Gen, Boy Groups, Legends, SM Entertainment";
        list.add(card("idol_exo_baekhyun", "Baekhyun", "Byun Baek-hyun", "EXO", false, "South Korea", "1992-05-06", "Main Vocal, Genius Idol", "#60A5FA", "R&B vocal king and multi-million seller ('Bambi', 'Pineapple Slice').", exoShineeCol));
        list.add(card("idol_exo_kai", "Kai", "Kim Jong-in", "EXO", false, "South Korea", "1994-01-14", "Main Dancer, Center", "#60A5FA", "World-renowned dance icon ('Mmmh', 'Rover').", exoShineeCol));
        list.add(card("idol_exo_do", "D.O.", "Doh Kyung-soo", "EXO", false, "South Korea", "1993-01-12", "Main Vocal, Actor", "#60A5FA", "Soulful R&B vocalist with effortless vocal runs.", exoShineeCol));
        list.add(card("idol_exo_chanyeol", "Chanyeol", "Park Chan-yeol", "EXO", false, "South Korea", "1992-11-27", "Main Rapper, Sub Vocal, Multi-Instrumentalist", "#60A5FA", "Tall deep-voiced rapper, guitarist, and producer.", exoShineeCol));
        list.add(card("idol_shinee_taemin", "Taemin", "Lee Tae-min", "SHINee", false, "South Korea", "1993-07-18", "Main Dancer, Vocalist, Soloist", "#2DD4BF", "'Idol of Idols' — legendary soloist and dance pioneer ('Move', 'Guilty').", exoShineeCol));
        list.add(card("idol_shinee_key", "Key", "Kim Ki-bum", "SHINee", false, "South Korea", "1991-09-23", "Lead Rapper, Lead Dancer, Vocal", "#2DD4BF", "Concept mastermind ('Gasoline', 'Pleasure Shop') and variety king.", exoShineeCol));
        list.add(card("idol_shinee_minho", "Minho", "Choi Min-ho", "SHINee", false, "South Korea", "1991-12-09", "Main Rapper, Visual", "#2DD4BF", "Flaming charisma athlete and actor.", exoShineeCol));

        // ==================== KISS OF LIFE (S2 Entertainment / 5th Gen) ====================
        String kiofColor = "#E11D48";
        String kiofCol = "5th Gen, Girl Groups, S2";
        list.add(card("idol_kiof_julie", "Julie", "Julie Han", "KISS OF LIFE", false, "USA / South Korea", "2000-03-29", "Leader, Main Rapper, Lead Dancer", kiofColor, "Charismatic Hawaii-born leader and rapper ('Sticky', 'Midas Touch').", kiofCol));
        list.add(card("idol_kiof_natty", "Natty", "Ahnatchaya Suputhipong", "KISS OF LIFE", false, "Thailand", "2002-05-30", "Main Dancer, Lead Rapper, Sub Vocal", kiofColor, "Y2K R&B diva ('Sugarcoat') with ten years of training.", kiofCol));
        list.add(card("idol_kiof_belle", "Belle", "Shim Hye-won", "KISS OF LIFE", false, "USA / South Korea", "2004-03-20", "Main Vocal, Composer", kiofColor, "Award-winning songwriter ('Unforgiven') and whistle-note vocal diva.", kiofCol));
        list.add(card("idol_kiof_haneul", "Haneul", "Won Ha-neul", "KISS OF LIFE", false, "South Korea", "2005-05-25", "Lead Vocal, Maknae", kiofColor, "Clear, stable live vocalist and cool maknae of KISS OF LIFE.", kiofCol));

        // ==================== ZEROBASEONE (ZB1 / WAKEONE 5th Gen) ====================
        String zb1Color = "#38BDF8";
        String zb1Col = "5th Gen, Boy Groups, WAKEONE";
        list.add(card("idol_zb1_hanbin", "Sung Hanbin", "Sung Han-bin", "ZEROBASEONE", false, "South Korea", "2001-06-13", "Leader, Center, Main Dancer, Vocal", zb1Color, "Tutting & waacking champion and beloved leader of ZB1.", zb1Col));
        list.add(card("idol_zb1_zhanghao", "Zhang Hao", "Zhang Hao", "ZEROBASEONE", false, "China", "2000-07-25", "Center, Main Vocal, Violinist", zb1Color, "Boys Planet #1 center with rich vocals and classical violin mastery.", zb1Col));
        list.add(card("idol_zb1_jiwoong", "Kim Jiwoong", "Kim Ji-woong", "ZEROBASEONE", false, "South Korea", "1998-12-14", "Vocalist, Dancer, Visual", zb1Color, "Sculpted actor-visual and eldest hyung of ZB1.", zb1Col));
        list.add(card("idol_zb1_matthew", "Seok Matthew", "Seok Woo-hyun", "ZEROBASEONE", false, "Canada", "2002-05-28", "Vocalist, Dancer", zb1Color, "Sunshine smile and groovy R&B vocals from Vancouver.", zb1Col));
        list.add(card("idol_zb1_ricky", "Ricky", "Shen Quanrui", "ZEROBASEONE", false, "China", "2004-05-20", "Vocalist, Visual", zb1Color, "'Young and Rich, Tall and Handsome' — webtoon prince come to life.", zb1Col));
        list.add(card("idol_zb1_gunwook", "Park Gunwook", "Park Gun-wook", "ZEROBASEONE", false, "South Korea", "2005-01-10", "Main Rapper, Main Dancer, Vocal", zb1Color, "Powerhouse all-rounder with explosive stage presence.", zb1Col));
        list.add(card("idol_zb1_yujin", "Han Yujin", "Han Yu-jin", "ZEROBASEONE", false, "South Korea", "2007-03-20", "Lead Dancer, Vocalist, Maknae", zb1Color, "Prodigy dancer maknae of ZEROBASEONE.", zb1Col));

        // ==================== STAYC (High Up Entertainment) ====================
        String staycColor = "#F472B6";
        String staycCol = "4th Gen, Girl Groups, High Up";
        list.add(card("idol_stayc_sieun", "Sieun", "Park Si-eun", "STAYC", false, "South Korea", "2001-08-01", "Co-Leader, Main Vocal", staycColor, "Piercing high notes and acclaimed actress ('Teddy Bear', 'ASAP').", staycCol));
        list.add(card("idol_stayc_isa", "Isa", "Lee Chae-young", "STAYC", false, "South Korea", "2002-01-23", "Lead Vocal, Lead Dancer, Visual", staycColor, "Dreamy R&B vocal tone and chic cat-like visual.", staycCol));
        list.add(card("idol_stayc_yoon", "Yoon", "Shim Ja-yoon", "STAYC", false, "South Korea", "2004-04-14", "Lead Vocal, Visual", staycColor, "Tall energetic vitamin with booming live vocals.", staycCol));
        list.add(card("idol_stayc_j", "J", "Jang Ye-eun", "STAYC", false, "South Korea", "2004-12-09", "Rapper, Vocalist, Maknae", staycColor, "Deep velvety contralto voice that surprises everyone.", staycCol));

        // ==================== YG Legends & Stars (2NE1, TREASURE, WINNER, iKON, AKMU, Lee Hi, PSY) ====================
        String ygCol = "YG Entertainment, Legends";
        list.add(card("idol_2ne1_cl", "CL", "Lee Chae-rin", "2NE1", false, "South Korea", "1991-02-26", "Leader, Main Rapper, Lead Vocal, Soloist", "#F59E0B", "'The Baddest Female' — legendary leader of 2NE1 ('I Am The Best', 'Hello Bitches').", "2nd Gen, Girl Groups, Soloists, " + ygCol));
        list.add(card("idol_2ne1_bom", "Park Bom", "Park Bom", "2NE1", false, "South Korea", "1984-03-24", "Main Vocal, Soloist", "#F59E0B", "Unmistakable emotional R&B vocal timbre of 2NE1 ('You and I', 'Spring').", "2nd Gen, Girl Groups, Soloists, " + ygCol));
        list.add(card("idol_2ne1_dara", "Sandara Park", "Sandara Park (Dara)", "2NE1", false, "South Korea / Philippines", "1984-11-12", "Vocalist, Visual, Actress", "#F59E0B", "Ageless visual icon and beloved stage vitamin of 2NE1.", "2nd Gen, Girl Groups, " + ygCol));
        list.add(card("idol_2ne1_minzy", "Minzy", "Gong Min-ji", "2NE1", false, "South Korea", "1994-01-18", "Main Dancer, Lead Vocal, Lead Rapper", "#F59E0B", "Explosive dance prodigy maknae of 2NE1.", "2nd Gen, Girl Groups, " + ygCol));

        String trCol = "4th Gen, Boy Groups, YG Entertainment";
        list.add(card("idol_treasure_hyunsuk", "Choi Hyunsuk", "Choi Hyun-suk", "TREASURE", false, "South Korea", "1999-04-21", "Co-Leader, Main Rapper, Main Dancer", "#38BDF8", "High-voltage hip-hop leader and producer of TREASURE ('JIKJIN', 'KING KONG').", trCol));
        list.add(card("idol_treasure_jihoon", "Jihoon (TREASURE)", "Park Ji-hoon", "TREASURE", false, "South Korea", "2000-03-14", "Co-Leader, Main Dancer, Lead Vocal", "#38BDF8", "Dynamic co-leader and powerhouse stage performer of TREASURE.", trCol));
        list.add(card("idol_treasure_yoshi", "Yoshi", "Kanemoto Yoshinori", "TREASURE", false, "Japan", "2000-05-15", "Rapper, Lyricist, Artist", "#38BDF8", "Melodic high-tone rapper and visual artist of TREASURE.", trCol));
        list.add(card("idol_treasure_junkyu", "Junkyu", "Kim Jun-kyu", "TREASURE", false, "South Korea", "2000-09-09", "Main Vocal, Visual", "#38BDF8", "Distinctive YG R&B vocal style and lovable koala smile.", trCol));
        list.add(card("idol_treasure_asahi", "Asahi", "Hamada Asahi", "TREASURE", false, "Japan", "2001-08-20", "Vocalist, Producer, Visual", "#38BDF8", "Artistic composer ('ORANGE', 'Thank You') and visual genius of TREASURE.", trCol));
        list.add(card("idol_treasure_jaehyuk", "Yoon Jaehyuk", "Yoon Jae-hyuk", "TREASURE", false, "South Korea", "2001-07-23", "Vocalist, Dancer", "#38BDF8", "Warm vocalist and charming heartthrob of TREASURE.", trCol));
        list.add(card("idol_treasure_doyoung", "Doyoung (TREASURE)", "Kim Do-young", "TREASURE", false, "South Korea", "2003-12-04", "Main Dancer, Vocalist", "#38BDF8", "Smooth dancer and trendy fashionista of TREASURE.", trCol));
        list.add(card("idol_treasure_haruto", "Haruto", "Watanabe Haruto", "TREASURE", false, "Japan", "2004-04-05", "Low-Tone Rapper, Visual", "#38BDF8", "Deep-voiced rapper and tall visual of YG's TREASURE ('DARARI').", trCol));
        list.add(card("idol_treasure_jeongwoo", "Park Jeongwoo", "Park Jeong-woo", "TREASURE", false, "South Korea", "2004-09-28", "Main Vocal", "#38BDF8", "Soulful main vocal prodigy of TREASURE with sky-high emotion.", trCol));
        list.add(card("idol_treasure_junghwan", "So Junghwan", "So Jung-hwan", "TREASURE", false, "South Korea", "2005-02-18", "Lead Dancer, Vocalist, Maknae", "#38BDF8", "Powerhouse maknae and center performer of TREASURE.", trCol));

        String winCol = "3rd Gen, Boy Groups, YG Entertainment";
        list.add(card("idol_winner_mino", "MINO", "Song Min-ho", "WINNER", false, "South Korea", "1993-03-30", "Main Rapper, Producer, Soloist", "#3B82F6", "YG hip-hop heavyweight and painter ('FIANCÉ', 'REALLY REALLY').", winCol + ", Soloists"));
        list.add(card("idol_winner_yoon", "Kang Seungyoon", "Kang Seung-yoon (YOON)", "WINNER", false, "South Korea", "1994-01-21", "Leader, Main Vocal, Producer", "#3B82F6", "Rock-soul vocalist and composer behind 'REALLY REALLY'.", winCol + ", Soloists"));

        String ikonCol = "3rd Gen, Boy Groups, YG Entertainment";
        list.add(card("idol_ikon_bobby", "BOBBY", "Kim Ji-won", "iKON", false, "South Korea", "1995-12-21", "Main Rapper, Vocalist, Soloist", "#EF4444", "Show Me The Money champion with raw raspy rap power ('LOVE SCENARIO').", ikonCol + ", Soloists"));
        list.add(card("idol_ikon_bi", "B.I", "Kim Han-bin", "iKON", false, "South Korea", "1996-10-22", "Producer, Rapper, Dancer, Soloist", "#EF4444", "Hitmaker behind 'LOVE SCENARIO' and 'BTBT'.", ikonCol + ", Soloists"));

        list.add(card("idol_akmu_suhyun", "Lee Suhyun", "Lee Su-hyun", "AKMU", false, "South Korea", "1999-05-04", "Main Vocal", "#10B981", "Crystal-clear fairy vocals of YG sibling duo AKMU ('Love Lee', 'How can I love the heartbreak').", ygCol));
        list.add(card("idol_akmu_chanhyuk", "Lee Chanhyuk", "Lee Chan-hyuk", "AKMU", false, "South Korea", "1996-09-12", "Main Producer, Vocalist", "#10B981", "Musical genius composer of AKMU.", ygCol));
        list.add(card("idol_solo_leehi", "Lee Hi", "Lee Ha-yi", "Soloist", true, "South Korea", "1996-09-23", "Soloist, Soul & R&B Vocalist", "#EC4899", "Unmatched soulful contralto voice ('Breathe', 'ONLY', 'Rose').", "Soloists, " + ygCol));
        list.add(card("idol_solo_psy", "PSY", "Park Jae-sang", "Soloist", true, "South Korea", "1977-12-31", "Soloist, Performer, Founder (P NATION)", "#FACC15", "Global K-Pop legend ('Gangnam Style', 'That That').", "Soloists, " + ygCol));

        // ==================== MAMAMOO, MONSTA X, THE BOYZ, ASTRO & Top Soloists ====================
        list.add(card("idol_mmm_hwasa", "Hwasa", "Ahn Hye-jin", "MAMAMOO", false, "South Korea", "1995-07-23", "Vocalist, Rapper, Soloist", "#EF4444", "Charismatic vocal powerhouse and solo queen ('Maria', 'I Love My Body').", "3rd Gen, Girl Groups, Soloists, RBW"));
        list.add(card("idol_mmm_solar", "Solar", "Kim Yong-sun", "MAMAMOO", false, "South Korea", "1991-02-21", "Leader, Main Vocal", "#EF4444", "Explosive vocal diva and fearless leader of MAMAMOO.", "3rd Gen, Girl Groups, RBW"));
        list.add(card("idol_mmm_moonbyul", "Moonbyul", "Moon Byul-yi", "MAMAMOO", false, "South Korea", "1992-12-22", "Main Rapper, Main Dancer, Vocal", "#EF4444", "Smooth baritone rapper and charismatic all-rounder.", "3rd Gen, Girl Groups, RBW"));
        list.add(card("idol_mx_im", "I.M", "Im Chang-kyun", "MONSTA X", false, "South Korea", "1996-01-26", "Lead Rapper, Producer, Maknae", "#8B5CF6", "Velvety deep voice and atmospheric R&B producer of MONSTA X.", "3rd Gen, Boy Groups, Starship"));
        list.add(card("idol_mx_joohoney", "Joohoney", "Lee Joo-heon", "MONSTA X", false, "South Korea", "1994-10-06", "Main Rapper, Vocalist, Producer", "#8B5CF6", "Ferocious spitfire rapper merged with cute 'Kukukaka' duality.", "3rd Gen, Boy Groups, Starship"));
        list.add(card("idol_tbz_juyeon", "Juyeon", "Lee Ju-yeon", "THE BOYZ", false, "South Korea", "1998-01-15", "Main Dancer, Vocalist, Visual", "#F43F5E", "Hypnotic main dancer and fashion icon of THE BOYZ.", "4th Gen, Boy Groups"));
        list.add(card("idol_tbz_sunwoo", "Sunwoo", "Kim Sun-woo", "THE BOYZ", false, "South Korea", "2000-04-12", "Main Rapper, Lead Dancer, Vocal", "#F43F5E", "Freestyle rap ace and quick-witted entertainer.", "4th Gen, Boy Groups"));
        list.add(card("idol_astro_eunwoo", "Cha Eun-woo", "Lee Dong-min", "ASTRO", false, "South Korea", "1997-03-30", "Vocalist, Visual, Actor", "#60A5FA", "'Face Genius' — legendary visual and global K-Drama star.", "3rd Gen, Boy Groups, Soloists"));
        list.add(card("idol_solo_bibi", "BIBI", "Kim Hyung-seo", "Soloist", true, "South Korea", "1998-09-27", "Soloist, R&B Singer-Songwriter, Actress", "#F43F5E", "Unfiltered R&B & pop storyteller ('Bam Yang Gang', 'BIBI Vengeance').", "Soloists, 4th Gen"));
        list.add(card("idol_solo_zico", "ZICO", "Woo Ji-ho", "Soloist", true, "South Korea", "1992-09-14", "Soloist, Rapper, Producer (KOZ / HYBE)", "#FACC15", "Hitmaker producer and founder of KOZ ('SPOT!', 'Any Song').", "Soloists, HYBE / KOZ, Legends"));
        list.add(card("idol_solo_eunbi", "Kwon Eunbi", "Kwon Eun-bi", "Soloist", true, "South Korea", "1995-09-27", "Soloist, Main Dancer, Main Vocal", "#38BDF8", "Undisputed 'Waterbomb Goddess' and powerhouse soloist ('Underwater').", "Soloists, 4th Gen, Girl Groups"));
        list.add(card("idol_solo_chuu", "Chuu", "Kim Ji-woo", "Soloist", true, "South Korea", "1999-10-20", "Soloist, Main Vocal", "#FB7185", "Human vitamin with powerhouse vocals ('Strawberry Rush').", "Soloists, 4th Gen, Girl Groups"));
        list.add(card("idol_solo_chungha", "Chung Ha", "Kim Chung-ha", "Soloist", true, "South Korea / USA", "1996-02-09", "Soloist, Main Dancer, Main Vocal", "#C084FC", "Solo dance & vocal queen ('Gotta Go', 'Roller Coaster', 'EENIE MEENIE').", "Soloists, Legends, MORE VISION"));
        list.add(card("idol_solo_sunmi", "Sunmi", "Lee Sun-mi", "Soloist", true, "South Korea", "1992-05-02", "Soloist, Vocalist, Bassist, Producer", "#EC4899", "'Sunmi-pop' pioneer ('Gashina', 'Siren', 'Heart Burn').", "Soloists, Legends"));
        list.add(card("idol_solo_youngji", "Lee Youngji", "Lee Young-ji", "Soloist", true, "South Korea", "2002-09-10", "Soloist, Main Rapper, Vocalist", "#10B981", "Powerhouse rapper & chart-topper ('Small Girl', 'Smoke').", "Soloists, 4th Gen"));
        list.add(card("idol_solo_jaypark", "Jay Park", "Park Jae-beom", "Soloist", true, "USA / South Korea", "1987-04-25", "Soloist, R&B / Hip-Hop Artist, Founder (MORE VISION)", "#F59E0B", "R&B & hip-hop icon ('Mommae', 'GANADARA', 'Taxi Blurr').", "Soloists, MORE VISION, Legends"));

        // ==================== LNGSHOT (MORE VISION / 5th Gen) ====================
        String lngColor = "#22D3EE";
        String lngCol = "5th Gen, Boy Groups, MORE VISION, LNGSHOT";
        list.add(card("idol_lngshot_ohyul", "Ohyul", "Kwon Oh-yul", "LNGSHOT", false, "South Korea", "2006-01-21", "Leader, Vocalist, Dancer", lngColor, "Charismatic leader of MORE VISION's boy group LNGSHOT.", lngCol));
        list.add(card("idol_lngshot_ryul", "Ryul", "Kim Ryul", "LNGSHOT", false, "South Korea", "2006-09-18", "Main Rapper, Dancer", lngColor, "Hip-hop & R&B specialist in LNGSHOT.", lngCol));
        list.add(card("idol_lngshot_woojin", "Woojin (LNGSHOT)", "Jeong Woo-jin", "LNGSHOT", false, "South Korea", "2008-03-08", "Vocalist, Producer, Dancer", lngColor, "All-rounder vocalist and songwriter of LNGSHOT.", lngCol));
        list.add(card("idol_lngshot_louis", "Louis", "Louis Elliot Jourdain Lim", "LNGSHOT", false, "South Korea / France", "2010-05-01", "Main Vocal, Maknae", lngColor, "Soulful R&B vocal prodigy and maknae of LNGSHOT.", lngCol));

        return list;
    }

    private static IdolCard card(String id, String stageName, String fullName, String groupName,
                                 boolean isSoloist, String country, String birthDate, String role,
                                 String accentColor, String bio, String collections) {
        return new IdolCard(id, stageName, fullName, groupName, isSoloist, country, birthDate, role, "", accentColor, bio, collections);
    }
}
