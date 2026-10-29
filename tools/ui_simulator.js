const fs = require('fs');
const path = require('path');

function parseStringsXml(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /<string\s+name="([^"]+)">([\s\S]*?)<\/string>/g;
    const map = {};
    let m;
    while ((m = regex.exec(content)) !== null) {
        map[m[1]] = m[2].trim();
    }
    return map;
}

const enStrings = parseStringsXml(path.join(__dirname, '../app/src/main/res/values/strings.xml'));
const esStrings = parseStringsXml(path.join(__dirname, '../app/src/main/res/values-es/strings.xml'));

const missingInEs = Object.keys(enStrings).filter(k => !(k in esStrings));
const seederContent = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/nitsutech/whosyourbias/data/StarterPackSeeder.java'), 'utf8');
const idolMatches = [...seederContent.matchAll(/card\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"/g)];

const groups = {};
for (const m of idolMatches) {
    const grp = m[4];
    groups[grp] = (groups[grp] || 0) + 1;
}

const report = {
    appName: enStrings.app_name,
    packageName: 'com.nitsutech.whosyourbias',
    i18nAudit: {
        enKeysCount: Object.keys(enStrings).length,
        esKeysCount: Object.keys(esStrings).length,
        missingInEs,
        status: missingInEs.length === 0 ? 'PASS (100% i18n parity)' : 'WARN'
    },
    starterPackStats: {
        totalPreloadedIdols: idolMatches.length,
        totalGroupsAndSoloPool: Object.keys(groups).length,
        groupsBreakdown: groups
    },
    simulatedVDomScreens: {
        MainActivity: ['Header(Gacha, Import)', 'YoutubeBgmBar(Play, Mute, VideoToggle, SongPicker)', 'ArenaTab(QuickMatch, PositionShowdowns, CustomMatches)', 'IdolsTab(Search, FilterChips, MultiSelectShare, PhotocardBinder)', 'GroupsTab(PlayGroupBattle, ShareGroup)', 'HallOfFameTab(GachaUnbox, TopBiasesLeaderboard)'],
        MatchActivity: ['TopMatchBar(RoundBadge, LightstickCheer)', 'YoutubeBgmBar', 'DuelArena(CardSlotTop[Front/Back3DFlip, PhotocardCycle], VSBadge, CardSlotBottom[Front/Back3DFlip, PhotocardCycle])', 'VictoryScreen(ConfettiOverlay, GoldenChampionCard, ShareVictoryPoster, ChampionshipPath)']
    }
};

console.log(JSON.stringify(report, null, 2));
if (missingInEs.length > 0) process.exit(1);
