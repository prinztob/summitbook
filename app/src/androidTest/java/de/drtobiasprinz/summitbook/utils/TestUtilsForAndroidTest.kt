package de.drtobiasprinz.summitbook.utils

import de.drtobiasprinz.summitbook.db.entities.Summit
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Utility class for Android instrumentation tests that reads test data from assets
 */
object TestUtilsForAndroidTest {

    fun getAllEntries(): List<Summit> {
        val entries = mutableListOf<Summit>()

        // Create a BufferedReader from the raw content string
        val content = getCsvContent()
        val reader = BufferedReader(InputStreamReader(content.byteInputStream()))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (
                        lineLocal != null &&
                        !lineLocal.startsWith("Activity") &&
                        !lineLocal.startsWith("required")
                    ) {
                        entries.add(Summit.parseFromCsvFileLine(lineLocal, "v0"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return entries
    }

    /**
     * Gets only current year entries for testing the year filter
     * The test needs to verify that exactly 85 entries are from the current year
     */
    fun getCurrentYearEntries(): List<Summit> {
        val allEntries = getAllEntries()
        return allEntries.filter { isCurrentYear(it) }
    }

    /**
     * Checks if a summit entry is from the current year
     */
    private fun isCurrentYear(summit: Summit): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val summitCalendar = java.util.Calendar.getInstance()
        summitCalendar.time = summit.date
        val summitYear = summitCalendar.get(java.util.Calendar.YEAR)

        return summitYear == currentYear
    }

    /**
     * Returns the contents of the summits.csv file
     *
     * Note: In our test scenario, we're assuming 85 entries are from the current year (2025)
     * and the rest are from previous years.
     */
    private fun getCsvContent(): String {
        return """
Activity Datum; Name; Sportart; Besuchte Orte; Land; Kommentare; Höhenmeter; Entfernung (km); Dauer (sec); Höchstgeschw. (km/h); Maximale Höhe (hm); Latitude; Longitude; Teilnehmer; activityId; isFavorite
required; required; Optional; Optional; Optional; Optional; required; required; Optional; Optional; Optional; Optional; Optional; Optional; Optional; required;
2023-04-08;Hahnenkamm;Mountainbike;1680967177515;39.5;8618;870;444;54.1;50.07819422520697;9.10960391163826;0;0;;;Peter's Bike;Klein-Krotzenburg;Deutschland
2023-04-05;Hohes Brett;Skitour;1680698363686;9.66;18303;1565;2340;40.8;47.58442615158856;13.049582540988922;0;1;;Florian Seilmeier;K2-Ski;;Deutschland
2023-03-31;Münster;Racer;1690297548441;46.95;6441;330;656;50.4;47.962695909664035;11.79295789450407;0;0;;;Rose;Egmating;Deutschland
2023-03-28;Ebertshausen;Racer;1690297563580;38.31;5135;325;691;60.1;47.96615369617939;11.538878520950675;0;0;;;Rose;Unterbiberg;Deutschland
2023-03-24;Peretshofer Höhe;Racer;1679655816308;64.76;8472;625;727;58.4;47.87383206188679;11.542704021558166;0;0;;;Rose;Unterbiberg;Deutschland
2023-03-22;Ebertshausen;Racer;1679489432001;38.31;4677;325;680;60.6;47.96645192429423;11.538892351090908;0;0;;;Rose;Unterbiberg;Deutschland
2023-03-18;Stöttwang;Racer;1679134629811;89.95;12950;1025;743;66.9;47.91049417108297;10.761159025132656;0;0;;;Rose;Unterbiberg,Andechs;Deutschland
2023-03-14;30s-Sprintintervalle;IndoorTrainer;1678799583713;32.98;3576;0;0;66.2;;;0;0;;;Rose;;
2023-03-11;Zugspitzblick;Hike;1678544329068;17.39;12039;1215;1697;16.7;47.349206479266286;11.099826553836465;0;0;;;;Rauthhütte;Österreich
2023-03-09;Perlacher Forst;Racer;1678384815931;28.99;3738;145;626;48.1;48.012061631307006;11.532316245138645;0;0;20s Intervalle in Zone 6;;Rose;Unterbiberg;Deutschland
2023-03-06;Perlacher Forst;Racer;1678121654253;28.99;3906;145;631;42.4;48.012337731197476;11.531796986237168;0;0;30s-Sprintintervalle;;Rose;Unterbiberg;Deutschland
2023-03-04;Münster;Racer;1677958956891;48.68;6534;330;656;50.5;47.96266883611679;11.793941427022219;0;0;;;Rose;Unterbiberg,Egmating ;Deutschland
2023-03-02;Ebertshausen;Racer;1677762829214;38.67;4952;345;685;49.3;47.96590902842581;11.538911294192076;0;0;;;Rose;Unterbiberg;Deutschland
2023-02-26;Pforzheimer Hütte;Skitour;1677415622755;9.29;4757;265;2566;29.0;47.149833189323545;11.06275306083262;0;0;Defekte Bindung und schlechte Sicht ;Hanno Kaupp,Christian Kellner,Sonja Kellner;K2-Ski;Pforzheimer Hütte,ac_id:1677341766978;Österreich
2023-02-25;Schartlkopf und Tal;Skitour;1677341766978;13.09;10710;1140;2831;28.0;47.1443956810981;11.062904857099056;0;1;Hintere Sonnenwand nicht bis zum Gipfel wegen Zeitmangel;Hanno Kaupp,Christian Kellner;K2-Ski;Pforzheimer Hütte,Samerschlag,ac_id:1677253801145;Österreich
2023-02-24;Schartlkopf;Skitour;1677253801145;11.64;10245;1330;2831;29.2;47.144397273659706;11.062960093840957;0;1;;Hanno Kaupp;K2-Ski;Sankt Sigmund;Österreich
2023-02-21;Ebertshausen;Racer;1676981565557;51.04;6904;525;684;56.8;47.96639911830425;11.538896290585399;0;0;;;Rose;Unterbiberg;Deutschland
2023-02-20;Brauneck;Skitour;1676889114869;8.36;6031;840;1555;59.7;47.66409865580499;11.524448739364743;0;1;;Peter Müller;K2-Ski;Lenngries;Deutschland
2023-02-17;Ebertshausen;Racer;1676629546639;38.85;5417;345;687;52.1;47.96569109894335;11.538910623639822;0;0;;;Rose;Unterbiberg;Deutschland
2023-02-15;Peretshofer Höhe;Racer;1676486769029;67.82;10117;670;739;56.6;47.87402903661132;11.542592123150826;0;0;;;Rose;Unterbiberg;Deutschland
2023-01-29;Ochsenkopf ;Skitour;1675008658849;21.68;20012;1585;2469;39.6;47.2756361681968;12.078605946153402;0;1;;Hanno Kaupp,Christian Kellner,Florian Seilmeier;K2-Ski;Pallspitz;Österreich
2023-01-22;Steinberg ;Skitour;1674457926700;10.93;8444;1010;1887;26.3;47.358505949378014;12.188219781965017;0;1;1 Stunde länger Anfahrt wegen Google Maps ;Florian Seilmeier;K2-Ski;Kitzbühler Alpen;Österreich
2023-01-18;8-Min-Tempointervalle;IndoorTrainer;1674050822426;15.02;3079;0;0;46.3;;;0;0;;;;;
2023-01-16;5-Min-Intervalle an der Schwelle;IndoorTrainer;1674050825535;16.36;2981;0;0;30.0;;;0;0;;;;;
2023-01-13;Perlacher Forst;Bicycle;1673637089296;29.49;4340;145;620;38.0;48.01254199817777;11.531474869698286;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2023-01-11;5-Min-Intervalle an der Schwelle;IndoorTrainer;1673445417438;20.01;3004;0;0;55.8;;;0;0;;;;;
2023-01-09;1-Minuten-Intervalle;IndoorTrainer;1673270955724;22.38;3120;0;0;34.63;;;0;0;;;;;
2023-01-03;Haag;Mountainbike;1672767423528;53.4;11106;880;416;61.5;50.04489325918257;9.11031587049365;0;0;zusätzlich nach Seligenstadt und zurück ;;Peter's Bike;Klein-Krotzenburg;Deutschland
2023-01-01;Rückersbach;Mountainbike;1672569729472;39.93;9340;750;338;62.0;50.03588958643377;9.094956871122122;0;0;;;Peter's Bike;Klein-Krotzenburg;Deutschland
2022-12-20;Längenfelderkopf;Skitour;1671613243060;16.35;10860;1275;1910;51.3;47.44213798083365;11.057120421901345;0;1;;Hanno Kaupp;K2-Ski;Garmisch-Partenkirchen;Deutschland
2022-12-12;Fitnesstest (3/20 min);IndoorTrainer;1670873156599;35.29;3755;0;0;71.8;;;0;0;;;;;
2022-12-06;Fitnesstest (10 Minuten);IndoorTrainer;1670413985696;25.75;3593;0;0;53.9;;;0;0;;;;;
2022-11-25;Peretshofer Höhe;Racer;1669372595215;64.86;9050;545;749;62.9;47.90245592594147;11.578774200752378;0;0;;;Rose;;Deutschland
2022-11-23;Perlacher Forst;Racer;1669224697013;44.63;5610;225;634;42.4;48.0121829174459;11.532073337584734;0;0;;;;;
2022-11-11;Taubenberg;Racer;1668193114126;95.24;13853;825;876;63.6;47.82772455364466;11.757190804928541;0;0;Platten - deshalb mit Auto von Mangfall nach Glonn ;;Rose;Glonn;Deutschland
2022-11-06;Mangfalltal;Hike;1667805856510;5.47;4262;145;640;6.25;47.90913177654147;11.780827604234219;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2022-11-03;Perlacher Forst;Racer;1667484966692;44.41;6460;225;630;40.5;48.01241174340248;11.531770918518305;0;0;;Joshua Prinz;Rose,Thule;;Deutschland
2022-10-30;Erding;Racer;1667149965935;26.71;3922;160;522;50.0;48.253932762891054;12.001895271241665;0;0;;Hanno Kaupp,Tino Wendler;Rose;;Deutschland
2022-10-28;Glonn;Racer;1666969645158;84.16;11608;580;711;54.9;47.84516368992627;11.75487857311964;0;0;;;Rose;;Deutschland
2022-10-26;Perlacher Forst;Racer;1666790293392;44.5;6612;225;629;41.4;48.012506207451224;11.531632533296943;0;0;;Joshua Prinz;Rose,Thule;;Deutschland
2022-10-21;Demeljoch ;BikeAndHike;1666465233213;36.1;15601;1290;1904;0.0;47.546137450262904;11.586415898054838;0;1;;Hanno Kaupp,Christian Kellner;Canyon-MTB;;Österreich,Deutschland
2022-10-17;Wallberg;Hike;1666072433295;5.22;3820;450;1722;7.09;47.66592767089605;11.796692786738276;0;1;;Helga Prinz,Rainer Prinz,Joshua Prinz,Jonah Prinz;;Setzberg;Deutschland
2022-10-16;Perlacher Forst;Racer;1665943184875;48.45;5923;225;631;46.8;48.01239891909063;11.531803356483579;0;0;;;;;
2022-10-09;Aueralm;Mountainbike;1665322307657;64.96;12086;980;1284;49.4;47.68709013238549;11.665932331234217;0;0;;;Canyon-MTB;;Deutschland
2022-10-06;Oberbiberg;Racer;1665052046365;30.44;4981;185;627;35.4;47.98196364194155;11.571061676368117;0;0;;Joshua Prinz;Rose,Thule;;Deutschland
2022-10-05;Perlacher Forst;Racer;1665052049455;22.62;4134;110;590;32.9;48.02619243040681;11.58258218318224;0;0;;Joshua Prinz;Rose,Thule;;Deutschland
2022-10-01;Münster;Racer;1664647833757;54.37;7378;505;659;53.1;47.962565990164876;11.794354068115354;0;0;Platten ;;Rose;;Deutschland
2022-09-25;Peretshofer Höhe;Racer;1664101894346;62.22;8374;615;740;64.1;47.87392711266875;11.542667476460338;0;0;;;Rose;;Deutschland
2022-09-24;Grüne Grotte;Hike;1664044090074;3.26;2768;155;916;6.08;47.680734638124704;12.013197261840105;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;Bayerischzell;Deutschland
2022-09-23;Schliersbergalm;Hike;1664043835358;5.56;4399;275;1064;5.54;47.74050196632743;11.875098450109363;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;Schliersee;Deutschland
2022-09-18;Neubiberg Radfahren;Racer;1663503060541;30.14;3881;115;595;38.1;48.03469964303076;11.54621603898704;0;0;;;Rose;;Deutschland
2022-09-13;Münster;Bicycle;1663091954178;10.85;1731;140;634;42.2;47.9877735581249;11.813236735761166;0;0;;;;Egmating ;Deutschland
2022-09-13;Rauhkopf;Hike;1663091952246;1.64;788;180;1640;52.8;47.66252336092293;11.920038778334856;0;1;;Joshua Prinz,Jonah Prinz,Rainer Prinz;;;Deutschland
2022-09-09;Kloster Schäftlarn;Racer;1662749550814;53.85;8004;520;687;46.1;47.97598256729543;11.492450991645455;0;0;;;Canyon-MTB;;Deutschland
2022-09-07;Brauneck;Hike;1662553654071;5.15;5011;870;1555;6.25;47.66414953395724;11.524706399068236;0;1;;Joshua Prinz;;;Deutschland
2022-09-06;Poing;Racer;1662553640425;41.24;5710;185;550;40.2;48.08299767784774;11.632807552814484;0;0;;;Rose;;Deutschland
2022-09-02;Ebersberg;Racer;1662145283988;63.18;8424;300;587;47.5;48.09550054371357;11.933505311608315;0;0;;;Rose;Unterbiberg;Deutschland
2022-08-28;Hahnenkamm;Mountainbike;1661682619015;55.37;10863;1255;438;62.7;50.07819766178727;9.109550770372152;0;0;;;;Klein-Krotzenburg;Deutschland
2022-08-24;Haag;Mountainbike;1661364269637;44.58;7308;500;405;56.0;50.07442865520716;8.965677246451378;0;0;;;;Klein-Krotzenburg;Deutschland
2022-08-21;Egmating ;Racer;1661364271127;34.11929;4441;82;597;42.1;48.01449473015964;11.745511796325445;0;0;;;Rose,Canyon-MTB;;Deutschland
2022-08-19;Dalfazer Wasserfall ;Hike;1660916805248;5.12;4042;325;1176;6.31;47.44136290624738;11.735873697325587;0;0;;;;;
2022-08-17;Achensee;Hike;1660754242241;7.34;5360;350;1270;36.5;47.425246853381395;11.70333799906075;0;0;;Joshua Prinz;;Achensee;Österreich
2022-08-16;Gschöllkopf;Hike;1660672667325;2.17;1896;215;2017;7.02;47.44699755683541;11.763670518994331;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Lene Päßler;;;Österreich
2022-08-16;Hochiss ;Hike;1660672662867;11.94;7830;1565;2299;13.0;47.45845327153802;11.764815570786595;0;1;;;;Maurach;Österreich
2022-08-15;Weißenbachklamm;Hike;1660672676513;6.54;6212;180;958;5.81;47.417488899081945;11.740439739078283;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Lene Päßler;;Maurach;Österreich
2022-08-14;Dalfazer Wasserfall ;Hike;1660496808479;10.04;6656;280;1192;50.4;47.44215072132647;11.735152853652835;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Lene Päßler;;Achensee;Österreich
2022-08-11;Sulzberg;Hike;1660242915555;7.83;6792;260;933;36.2;47.64782376587391;10.36276699975133;0;0;inkl. Rottachsee;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;Sulzberg;Deutschland
2022-08-09;Kappeler Alm;Hike;1660067789217;5.0;3571;500;1342;7.49;47.601353572681546;10.51372155547142;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2022-08-08;Burgkranzegger Horn;Hike;1660067792193;12.57;7370;455;1149;8.83;47.630059495568275;10.406846841797233;0;1;und zum Rottachsee ;Joshua Prinz;;Moosbach;Deutschland
2022-08-07;Kohlenberg ;Hike;1659892413788;15.79;11460;365;983;7.66;47.661988427862525;10.381301064044237;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;Geratser Wasserfall,Moosbach;Deutschland
2022-08-04;Östliche Karwendelspitze ;BikeAndHike;1659639753164;38.22;13870;1820;2537;51.9;47.444762606173754;11.421211184933782;0;1;;Hanno Kaupp;Canyon-MTB;;Deutschland,Österreich
2022-07-29;Rechelkopf;BikeAndHike;1659092742742;56.97;10658;1090;1330;50.6;47.722334107384086;11.62484516389668;0;1;;;Scott;Otterfing,Holzkirchen,Mariastein;Deutschland
2022-07-27;Bodenschneidhaus;Hike;1658943500388;9.56;8374;565;1365;9.24;47.67802837304771;11.839208481833339;0;0;;Mirjam Prinz,Joshua Prinz;;Neuhaus;Deutschland
2022-07-26;Dietersheim;Racer;1658943489310;34.81;4624;125;545;42.0;48.082477329298854;11.632834877818823;0;0;;;Rose;;Deutschland
2022-07-22;München;Racer;1658607037231;41.84;6071;205;589;46.7;48.04199516773224;11.548119150102139;0;0;;;Rose;Unterbiberg;Deutschland
2022-07-18;Baumgartenschneid;Hike;1658159837232;12.9;10979;740;1448;7.15;47.70250285975635;11.808865824714303;0;1;;Joshua Prinz;;Tegernsee,Schliersee;Deutschland
2022-07-15;Steinfalk;BikeAndHike;1657910775668;36.73;14563;1875;2346;60.4;47.42710193619132;11.512568816542625;0;1;;Hanno Kaupp;Canyon-MTB;Hinterriß,Lazisköpfl,Mahnkopf;Österreich
2022-07-12;Peretshofer Höhe;Racer;1657629431750;62.34;8082;615;742;66.4;47.87402853369713;11.542661776766181;0;0;;;Rose;;Deutschland
2022-07-07;Grünwald ;Mountainbike;1657194624351;29.51;4349;200;590;37.3;48.040988920256495;11.52487319894135;0;0;;;Canyon-MTB;München;Deutschland
2022-07-06;Neureuther Hütte;Hike;1657115642051;6.05;6444;525;1261;4.8;47.72856538183987;11.772292563691735;0;0;;Joshua Prinz,Mirjam Prinz;;Tegernsee;Deutschland
2022-07-04;München;Bicycle;1657005986413;18.53;2625;105;545;46.9;48.09660645201802;11.653405409306288;0;0;;;;;Deutschland
2022-07-03;Münster;Racer;1656872623184;57.17;8226;430;644;52.2;47.96194992028177;11.796608045697212;0;0;;;Rose;;Deutschland
2022-07-02;München;Bicycle;1656786440928;23.13;5927;115;557;42.8;48.085531275719404;11.594644328579307;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;Scott;;Deutschland
2022-06-30;Peretshofer Höhe;Racer;1656594143489;62.19;8442;615;741;63.1;47.875626711174846;11.542576616629958;0;0;;;Rose;;Deutschland
2022-06-27;Taufkirchen ;Bicycle;1656594145365;17.65;4091;85;559;31.6;48.05158264003694;11.605258164927363;0;0;;;Scott;;
2022-06-24;Kochberg ;Hike;1656097307997;6.89;5277;250;440;7.09;46.88818839378655;15.76301783323288;0;0;;Joshua Prinz;;;Österreich
2022-06-23;Kochberg ;Hike;1656077421909;8.53;6465;320;445;7.35;46.89326003193855;15.751757752150297;0;0;;Joshua Prinz;;;Österreich
2022-06-22;Silberberg ;Hike;1656077434652;1.24;1365;80;376;5.17;46.7804413754493;15.5121651571244;0;0;;Mirjam Prinz,Jonah Prinz,Joshua Prinz,Peter Appel,Christa Appel,Berni;;;Österreich
2022-06-21;Kochberg ;Hike;1655839218073;6.32;4452;215;432;6.42;46.88817523419857;15.763111794367433;0;0;;Joshua Prinz;;;Österreich
2022-06-20;Kochberg ;Hike;1655839222974;3.08;2711;150;440;6.35;46.888058139011264;15.763135012239218;0;0;;Joshua Prinz;;;Österreich
2022-06-16;Köpperl;Climb;1655402196049;11.47;10022;650;1741;12.2;47.59163064882159;14.145129304379225;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Sonja Westermeyer,Fabian Knopf;;Schneehitz;Österreich
2022-06-15;Burgruine Wolkenstein;Hike;1655325360077;8.99;8562;425;906;6.18;47.561394441872835;14.157116347923875;0;0;;Joshua Prinz,Jonah Prinz,Sonja Westermeyer,Mirjam Prinz,Fabian Knopf;;Ems,Wörschach;Österreich
2022-06-15;Hohe Trett;Climb;1655290368528;12.27;7916;655;1681;11.1;47.519227769225836;14.204508466646075;0;1;Sonnenaufgang;Sonja Westermeyer;;;Österreich
2022-06-14;Burgruine Wolkenstein;Hike;1655290365573;3.3;3211;160;756;12.4;47.556517850607634;14.151339456439018;0;0;;Joshua Prinz;;Wörschach;Österreich
2022-06-14;Aicherlstein;Climb;1655204833685;6.11;5275;535;1180;7.32;47.556964019313455;14.135586759075522;0;1;;Joshua Prinz;;Wörschach;Österreich
2022-06-13;Wörschachklamm;Hike;1655143446289;8.97;7051;640;999;8.7;47.560710394755006;14.139161137863994;0;0;;Joshua Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf,Mirjam Prinz;;Wörschach;Österreich
2022-06-12;Burgruine Wolkenstein;Hike;1655057957942;7.07;6769;375;860;6.21;47.55977187305689;14.154715267941356;0;0;;;;;Österreich
2022-06-11;Burgruine Wolkenstein;Hike;1655057959417;3.14;2983;250;771;5.98;47.55762015469372;14.152247468009591;0;0;;;;;Österreich
2022-06-05;Egmating;Racer;1654504720403;19.61;3714;155;609;33.5;48.00932242535055;11.799935158342123;0;0;;;Rose;;Deutschland
2022-05-27;Ottmannsberg;Hike;1653761841008;10.61;8966;235;482;7.22;49.14696444757283;10.927158426493406;0;0;;;;;Deutschland
2022-05-26;Streckfuß;Hike;1653587589535;9.89;6913;240;497;7.32;49.14626304991543;10.946113849058747;0;0;;Joshua Prinz;;Ottmannsberg;Deutschland
2022-05-25;Ottmannsberg;Hike;1653502483344;7.87;6146;175;479;6.25;49.14753383025527;10.929891848936677;0;0;;;;;Deutschland
2022-05-24;Ottmannsberg;Hike;1653462799611;6.47;6178;160;475;5.44;49.14698506705463;10.926933037117124;0;0;;;;;Deutschland
2022-05-23;Ottmannsberg;Hike;1653462795488;5.27;4263;150;491;12.9;49.14568184874952;10.927038732916117;0;0;;;;;Deutschland
2022-05-22;Ottmannsberg;Hike;1653462803854;1.27;1037;90;455;6.72;49.14586842991412;10.926615111529827;0;0;;;;;Deutschland
2022-05-18;Neubiberg Training;Racer;1652898966836;31.24;3619;170;620;42.6;48.01868316717446;11.530658388510346;0;0;;;Rose;;Deutschland
2022-05-15;Rechelkopf;BikeAndHike;1652637927754;61.06;10818;1210;1330;53.8;47.722310554236174;11.624772241339087;0;1;;;Canyon-MTB;Otterfing,Mariastein;Deutschland
2022-05-11;Tregler Alm;Hike;1652339171373;9.02;7105;345;1130;11.1;47.750493446365;11.977437045425177;0;0;;Mirjam Prinz,Helga Prinz,Rainer Prinz,Jonah Prinz,Joshua Prinz;;;Deutschland
2022-05-09;Neubiberg Training;Racer;1652812687155;30.6;5037;170;557;45.9;48.085311921313405;11.59393010661006;0;0;;;;;Deutschland
2022-05-06;Neubiberg Training;Racer;1651863416882;34.23;4056;160;597;48.4;48.04151530377567;11.548071457073092;0;0;;;Rose;;Deutschland
2022-05-02;Anstieg Col de Galibier;IndoorTrainer;1651931679635;8.93;2543;590;2637;47.9;45.0640029553324;6.407949971035123;0;0;;;;;
2022-04-30;Egmating;Bicycle;1651341597142;35.22;5578;110;610;31.1;48.009131317958236;11.799924094229937;0;0;;;Scott,Thule;;Deutschland
2022-04-29;Peretshofer Höhe;Racer;1651262558035;65.67;8513;720;742;63.5;47.87419365718961;11.542550129815936;0;0;;;Rose;Unterbiberg;Deutschland
2022-04-27;Anstieg Col de Galibier;IndoorTrainer;1651262556399;9.43;2806;594;2637;42.4;45.0640029553324;6.407949971035123;0;0;;;;;
2022-04-22;Hahnenkamm;Mountainbike;1650647016559;50.44046843046689;10521;1425;443;59.0;50.07819707505405;9.109579101204872;0;0;;;;Klein-Krotzenburg;Deutschland
2022-04-19;Rückersbach;Mountainbike;1650389543544;44.81;8411;970;357;64.7;50.03594398498535;9.094951255246997;0;0;;;;Klein-Krotzenburg;Deutschland
2022-04-15;Hahnenkamm;Mountainbike;1650087026434;48.86;10464;1185;443;57.1;50.07816673256457;9.109611790627241;0;0;Hahnenkamm: 1:01:25,1. 5:00/22:45,2. 4:33/23:30;;;Klein-Krotzenburg;Deutschland
2022-04-06;Training;IndoorTrainer;1649308700298;10.93;1254;0;0;58.3;;;0;0;;;;;
2022-04-04;Training - Pyramiden;Racer;1649078585312;27.71;3637;135;591;40.9;48.050395930185914;11.548919873312116;0;0;;;Canyon-MTB;;Deutschland
2022-03-30;Training;IndoorTrainer;1648665316620;12.75;2143;0;0;32.1;;;0;0;;;;;
2022-03-28;Training - Pyramiden;Racer;1648488789432;24.05;2952;120;581;43.3;48.047174010425806;11.571364430710673;0;0;;;Canyon-MTB;;Deutschland
2022-03-26;Jakobsbaiern;Hike;1648488795882;3.26;3315;80;602;5.51;47.95425759628415;11.908782720565796;0;0;;Sonja Westermeyer,Mirjam Prinz,Jonah Prinz,Joshua Prinz,Katrin Izaak;;Piusheim;Deutschland
2022-03-25;Peretshofer Höhe;Racer;1648235389696;68.25;9927;730;744;55.9;47.87401386536658;11.542637888342142;0;0;;Tino Wendler;Canyon-MTB;Unterbiberg;Deutschland
2022-03-23;Fitnesstest (20 Minuten);Racer;1648058420566;19.73;2594;55;555;45.8;48.07261526584625;11.63711509667337;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2022-03-21;Intervall-Training;Bicycle;1647894896001;14.84;2225;50;557;41.8;48.06391376070678;11.653129812330008;0;0;;;Canyon-MTB;Unterbiberg;
2022-03-18;Anstieg Col de Tourmalet;IndoorTrainer;1647626573189;12.39;3711;822;1538;38.2;42.90627853944898;0.1058365311473608;0;0;;;;;
2022-03-17;Indoor Cycling;IndoorTrainer;1647551129304;17.83;3016;0;2;41.0;;;0;0;;;;;
2022-03-14;Rauthhütte;Hike;1647278800694;6.8;3563;500;1605;18.1;47.34556596726179;11.104357559233904;0;0;;TNG;;Interalpen Hotel;Österreich
2022-03-13;Seefelder Spitze;Skitour;1647186835895;10.54;7382;1010;2209;61.2;47.332801669836044;11.237655971199274;0;1;;TNG;K2-Ski;Seefelder Joch;Österreich
2022-03-09;Bergintervalle: Leistungsspitzen;IndoorTrainer;1648488791605;27.82;2686;0;0;46.8;;;0;0;;;;;
2022-03-06;Namenloser Gipfel;Skitour;1646574434310;16.5;11314;975;3193;55.7;46.483651930466294;10.610536728054285;0;1;;ZHS;K2-Ski;Zufallhütte,ac_id:1646496869178;Italien
2022-03-05;Eisseespitze;Skitour;1646496869178;15.36;11568;1040;3230;39.8;46.48038818500936;10.608855485916138;0;1;;ZHS;K2-Ski;Zufallhütte,ac_id:1646411833829;Italien
2022-03-04;Monte Cevedale;Skitour;1646411833829;19.1;22920;1550;3767;49.6;46.44372448325157;10.616745539009571;0;1;;ZHS;K2-Ski;Zufallhütte,ac_id:1646327847291;Italien
2022-03-03;Cima Marmotta;Skitour;1646327847291;13.47;17319;1150;3309;58.5;46.449608160182834;10.68291555158794;0;1;;ZHS;K2-Ski;Zufallhütte,ac_id:1646240406280;Italien
2022-03-02;Zufallhütte Aufstieg;Skitour;1646240406280;9.31;7431;745;2753;29.2;46.47690776735544;10.650672214105725;0;0;;ZHS;K2-Ski;Zufallhütte;Italien
2022-02-28;20s in Zone 5 und 40s in Zone 3;IndoorTrainer;1646073461028;44.68;3889;0;0;68.0;;;0;0;;;;;
2022-02-25;Fitnesstest (20 Minuten);IndoorTrainer;1645808064508;39.46;3637;0;0;65.7;;;0;0;;;;;
2022-02-21;Fitnesstest (3/10 min);IndoorTrainer;1645464081719;37.34;3632;0;0;62.0;;;0;0;;;;;
2022-02-18;Kotalmjoch;Skitour;1645195939034;14.1;9780;1215;2157;32.7;47.46553388424218;11.751720272004604;0;1;;Hanno Kaupp;K2-Ski;Achensee;Österreich
2022-02-16;Anstieg Col de Tourmalet;IndoorTrainer;1645031360279;16.18;3600;596;2637;84.9;45.0640029553324;6.407949971035123;0;0;;;;;
2022-02-12;Münster;Mountainbike;1644681436177;31.74;5066;270;654;44.4;47.962650898844004;11.79406313225627;0;0;;;Canyon-MTB;Egmating;Deutschland
2022-02-11;Anstieg Col de Tourmalet;IndoorTrainer;1644602006651;18.75;6048;1417;2116;58.0;42.90832766331732;0.1452225912362337;0;0;;;;;
2022-02-10;Mountainbiken;IndoorTrainer;1644519482871;23.63;3577;0;0;58.5;;;0;0;;;;;
2022-02-06;Vorderunnütz;Skitour;1644166716196;13.73;10384;1310;2078;36.5;47.515062633901834;11.738567892462015;0;1;;Florian Seilmeier;K2-Ski;Schlagkopf,Kotalm,Steinberg am Rogan;Österreich
2022-02-04;Anstieg Col de Tourmalet;IndoorTrainer;1643997399855;18.74;6512;1411;2116;57.9;42.90837029775872;0.14553677056738934;0;0;;;;;
2022-02-03;1-Minuten-Intervalle;IndoorTrainer;1643908121298;33.18;3414;0;0;65.0;;;0;0;;;;;
2022-01-31;Freising Gehen;Hike;1644424110051;6.11;3914;165;496;15.1;48.41142822057009;11.729040434584022;0;0;erste Runde nicht aufgezeichnet;;;;Deutschland
2022-01-30;Taubenstein Bergstation;Skitour;1643543800357;10.13;7826;1135;1643;43.8;47.65971994266671;11.92028729424166;0;0;;Peter Müller;K2-Ski;;Deutschland
2022-01-26;Fitnesstest (20 Minuten);IndoorTrainer;1643211209631;30.66;2958;0;0;70.1;;;0;0;;;;;
2022-01-24;Fitnesstest (3/10 min);IndoorTrainer;1643108946867;30.73;3573;0;0;67.1;;;0;0;;;;;
2022-01-21;Anstieg Col de Tourmalet;IndoorTrainer;1642780844439;18.8;5947;1418;2116;49.0;;;0;0;;;;;
2022-01-19;Anstieg Col de Tourmalet;IndoorTrainer;1642595977234;12.01;3938;832;1544;37.0;;;0;0;;;;;
2022-01-17;Anstieg Alp d'Huez;IndoorTrainer;1642509555497;9.81;3600;799;1516;28.9;;;0;0;;;;;
2022-01-14;Anstieg Alp d'Huez;IndoorTrainer;1642509552973;9.2;3600;761;1476;24.1;;;0;0;;;;;
2022-01-12;5-Min-Intervalle an der Schwelle;IndoorTrainer;1642780840983;20.39;2751;0;0;39.3;;;0;0;;;;;
2022-01-10;1-Minuten-Intervalle;IndoorTrainer;1642780842119;26.09;3386;0;0;46.4;;;0;0;;;;;
2022-01-07;Zone 2 - Aerob;IndoorTrainer;1642780840215;14.24;2314;0;0;27.9;;;0;0;;;;;
2022-01-02;Hahnenkamm;Mountainbike;1641210888355;65.88;15706;1620;450;61.8;50.07837127760067;9.109753178728226;0;0;Hahnenkamm: 1:09:24 1. 5:06/25:11;;;Rückersbacher Schlucht;Deutschland
2021-12-17;Indoor cycling;IndoorTrainer;1651931680629;14.02;2421;423;0;46.8;;;0;0;;;;;
2021-12-15;Villar-d'Arêne Indoor Cycling;IndoorTrainer;1651931678351;8.79;2735;594;2637;52.5;45.0640029553324;6.407949971035123;0;0;;;;;
2021-12-13;Alp d'Huez;IndoorTrainer;1642509558688;9.46;3592;775;1495;30.3;;;0;0;;;;;
2021-12-10;Indoor Cycling;IndoorTrainer;1643543793317;14.46;2275;0;0;56.7;;;0;0;;;;;
2021-12-08;Neubiberg Radfahren;Bicycle;1643543797179;25.93;4025;94;557;34.3;;;0;0;;;Canyon-MTB;;Deutschland
2021-11-26;Ismaning;Bicycle;1637921132060;35.74;5775;135;547;30.9;;;0;0;;;Canyon-MTB;;Deutschland
2021-11-17;München Ost West;Bicycle;1637163203050;31.66;5533;160;560;50.1;48.085351986810565;11.593943098559976;0;0;;;Scott;;Deutschland
2021-10-15;Neubiberg Laufen;Running;78474275;8.03;2935;110;559;13.7;48.075539292767644;11.64757328107953;0;0;;;;;Deutschland
2021-10-03;Egmating;Bicycle;15672703;18.4;2565;86;610;46.1;48.009410351514816;11.799901630729437;0;0;;;Scott;;Deutschland
2021-09-17;Rund um das Sonnwendjoch;Mountainbike;120393665;44.53;13392;1485;1672;55.9;47.592652067542076;11.948914602398872;0;0;merge of Thiersee Mountainbiken, Bayrischzell Mountainbiken;Hanno Kaupp;Canyon-MTB;ac_id:67907176;Deutschland,Österreich
2021-09-17;Sonnwendjoch;Hike;67907176;2.49;3488;325;1986;7.56;47.598588299006224;11.949614575132728;0;1;;Hanno Kaupp;;;Deutschland
2021-09-11;Setzberg;Hike;41023307;1.72;2409;225;1706;4.9;47.6512435823679;11.7845378536731;0;1;;Christian Kellner;;ac_id:155457072;Deutschland
2021-09-11;Rund um den Schinder;Mountainbike;155457072;65.81;15900;1460;1498;66.5;47.65624271705747;11.789686940610409;0;0;;Christian Kellner;Canyon-MTB;;Deutschland,Österreich
2021-09-03;Peretshofer Höhe;Racer;171003019;65.81;9244;640;745;62.0;47.87403297610581;11.542627159506083;0;0;;;Rose;;Deutschland
2021-08-30;Hahnenkamm;Mountainbike;249073734;46.06;9106;1160;439;58.6;50.07820210419595;9.109594188630581;0;0;Hahnenkamm: 58:07, 1. 4:19/21:27, 2. 4:12/21:13;;Canyon-MTB;Klein-Krotzenburg;Deutschland
2021-08-28;Hahnenkamm;Mountainbike;45341955;45.88;9548;1160;444;53.4;50.07831232622266;9.109666859731078;0;0;;;Canyon-MTB;Klein-Krotzenburg;Deutschland
2021-08-20;Kandel;Mountainbike;75760419;28.8;8772;1070;1241;42.3;48.06250677444041;8.011366641148925;0;1;;Damian Bradley;Canyon-MTB;Glottertal;Deutschland
2021-08-18;Brombeerkopf;Mountainbike;211663315;11.38;3847;570;870;54.1;48.0234316829592;7.9689667001366615;0;0;;;Canyon-MTB;Glottertal;Deutschland
2021-08-18;Hilzinger Mühle;Hike;220647107;8.0;8045;185;448;0.0;48.03443477489054;7.990542808547616;0;0;inkl. Rewe;Mirjam Prinz,Jonah Prinz,Helga Prinz,Rainer Prinz;;Glottertal;Deutschland
2021-08-17;Silbergrüble;Hike;48284189;5.59;9231;200;490;8.56;48.039160910993814;7.938575679436326;0;0;;Mirjam Prinz,Jonah Prinz,Helga Prinz,Rainer Prinz;;Glottertal;Deutschland
2021-08-16;Kandel;Hike;140834033;1.71;2533;70;1234;5.07;48.062543235719204;8.011489352211356;0;1;;Mirjam Prinz,Helga Prinz,Rainer Prinz,Jonah Prinz;;;Deutschland
2021-08-16;Langeck;Mountainbike;208211586;18.52;4967;765;861;52.6;48.02103856578469;8.009469648823142;0;0;inkl. Rewe;;Canyon-MTB;Glottertal;Deutschland
2021-08-15;Weinbergrunde;Hike;181041109;5.73;7784;175;484;8.46;48.054935401305556;7.947264024987817;0;0;;Andreas Packinger,Mirjam Prinz,Jonah Prinz;;Glottertal;Deutschland
2021-08-14;Saalenberg;Hike;114104293;2.67;7227;105;483;42.2;47.92724523693323;7.814452098682523;0;0;;Mirjam Prinz,Jonah Prinz,Antje Thamm,Damian Bradley;;;Deutschland
2021-08-14;Kandel Höhenweg;Mountainbike;102150919;34.29;9697;575;993;48.9;48.037913516163826;8.063427731394768;0;0;;Damian Bradley;Canyon-MTB;;Deutschland
2021-08-11;Kandel;Mountainbike;238167412;23.26;4868;745;1226;68.7;48.06249177083373;8.011409053578973;0;1;;;Canyon-MTB;;Deutschland
2021-08-11;Zweribach Wasserfall;Hike;59568310;3.64;8854;175;983;6.08;48.04658048786223;8.077579904347658;0;0;;Mirjam Prinz,Jonah Prinz;;;Deutschland
2021-08-09;Kandel;Mountainbike;52470754;17.31;3897;565;1244;63.6;48.0626505240798;8.011494129896164;0;1;;;Canyon-MTB;;Deutschland
2021-08-08;Plattensee;Hike;143575833;4.99;3629;170;1002;13.4;48.04920067079365;8.072703899815679;0;0;;Mirjam Prinz,Jonah Prinz,Antje Thamm,Damian Bradley;;;Deutschland
2021-08-04;Peretshofer Höhe;Racer;212552684;65.35;8775;580;747;57.2;47.87381161004305;11.542687006294727;0;0;;;Rose;;Deutschland
2021-07-24;Erlspitze;Climb;174518954;15.83;11285;1025;2405;9.37;47.32011088170111;11.28505035303533;0;1;;Christian Kellner;;Kuhlochspitze,Freiungspitze,Freiungen Höhenweg,Zirler Klettersteig,ac_id:173013376,Nördlinger Hütte;Österreich
2021-07-23;Reither Spitze;Climb;173013376;7.54;7625;1260;2374;9.0;47.323163067921996;11.235976489260793;0;1;;Christian Kellner;;Nördlinger Hütte;Österreich
2021-07-17;Egmating;Bicycle;249723688;32.84;5113;82;595;30.5;48.00896292552352;11.79140456020832;0;0;;;Scott;;Deutschland
2021-07-14;Neubiberg Training;Racer;51872443;19.84;2516;90;581;43.8;48.05357116274536;11.54875391162932;0;0;;;Rose;;Deutschland
2021-07-12;Neubiberg - HIT;Racer;68756691;19.86;2518;95;578;44.5;48.04959889501333;11.571496110409498;0;0;;;Rose;;Deutschland
2021-07-11;Peretshofer Höhe;Racer;14874857;65.5;8682;640;732;63.0;47.87408888339996;11.542599834501743;0;0;;;Rose;;Deutschland
2021-07-03;Wildalpjoch;Climb;263848056;6.0;4227;695;1720;11.4;47.70133651793003;12.035406958311796;0;1;;;;KR-Dörfler-Hütte;Deutschland
2021-07-02;Wendelstein;Climb;171751634;8.5;5730;760;1838;8.23;47.7034792676568;12.012233007699251;0;1;;Tino Wendler;;KR-Dörfler-Hütte;Deutschland
2021-07-01;Aufstieg KR- Dörfler-Hütte;Hike;180624019;1.0;677;715;1207;14.9;47.70787867717445;12.038015993312001;0;0;;Enrico Schubert,Tino Wendler,Mirjam Prinz,Jonah Prinz;;;Deutschland
2021-06-27;Peretshofer Höhe;Racer;208333806;65.01;8700;640;752;59.8;47.87396457977593;11.54262749478221;0;0;1h 25.8;;Rose;;Deutschland
2021-06-20;Peretshofer Höhe;Racer;11093190;64.6;8457;580;743;58.7;47.87211419083178;11.536276862025261;0;0;;;Rose;;Deutschland
2021-06-14;Um die Reiteralm;Mountainbike;26858387;55.81;13021;1255;1323;61.8;47.556077633053064;12.785995490849018;0;0;;Hanno Kaupp;Canyon-MTB;Schneizlreuth;Deutschland,Österreich
2021-06-13;Um den Hohen Göll;Mountainbike;162613997;64.8;21246;2515;1736;72.0;47.574417823925614;13.04240595549345;0;0;;Hanno Kaupp;Canyon-MTB;;Deutschland,Österreich
2021-06-12;Warteck;Mountainbike;229061904;42.27;14248;1840;1741;69.3;47.54002788104117;12.993205022066832;0;1;;Hanno Kaupp;Canyon-MTB;Gotzenalm;Deutschland
2021-06-11;Um das Sonntagshorn;Mountainbike;82379426;47.64;15650;1620;1211;43.4;47.69002522341907;12.76420765556395;0;0;;Hanno Kaupp;Canyon-MTB;Ruhpolding;Deutschland,Österreich
2021-06-07;Raidling;Hike;80385683;11.11;7142;410;1918;14.5;47.59837003424764;14.147603055462241;0;1;;;;Schneehitz;Österreich
2021-06-06;Wörschach Wandern;Hike;1635272071261;1.58;786;53;1612;46.5;;;0;0;;;;;Österreich
2021-06-05;Köpperl und Aufstieg;Hike;265999901;9.16;6986;725;1746;0.0;47.591578429564834;14.14514816366136;0;0;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf;;Schneehitz;Österreich
2021-06-04;Aicherlstein;Hike;11920441;7.41;5378;575;1181;11.9;47.55694323219359;14.135840479284525;0;1;;Sonja Westermeyer,Mirjam Prinz,Jonah Prinz,Fabian Knopf;;Wörschach;Österreich
2021-05-20;Neubiberg Radfahren;Bicycle;149935876;17.18;2303;120;581;39.2;48.04670420475304;11.570294564589858;0;0;;;Canyon-MTB;;Deutschland
2021-05-16;Mangfalltal;Hike;67955786;5.65;4194;130;629;8.5;47.92608853429556;11.786103341728449;0;0;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf;;Grub;Deutschland
2021-05-15;Peretshofer Höhe;Mountainbike;173527283;65.17;9027;570;745;54.1;47.873996682465076;11.542627243325114;0;0;;;Canyon-MTB;;Deutschland
2021-05-10;Hochalm;Hike;175325074;7.71;6196;670;1349;7.12;47.60898454114795;11.604245211929083;0;1;;Mirjam Prinz;;Sylvensteinspeicher;Deutschland
2021-05-09;Rechelkopf;BikeAndHike;213300379;99.93;16969;1480;1330;66.6;47.722313571721315;11.624643495306373;0;1;;;Canyon-MTB;Otterfing,Mariastein,Taubenberg,Egmating;Deutschland
2021-05-04;Training - FTP;Bicycle;239572147;25.17;3303;135;583;40.5;48.04285909049213;11.548213195055723;0;0;;;Canyon-MTB;;Deutschland
2021-05-01;Münster;Mountainbike;157476030;40.5;7119;550;645;43.0;47.959857042878866;11.834884593263268;0;0;;;Canyon-MTB;Egmating;Deutschland
2021-04-30;Training;Bicycle;77663590;27.0;3660;120;595;43.1;48.03762501105666;11.549961660057306;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2021-04-28;Training - HIIT;Bicycle;142312845;23.69;3094;135;578;42.1;48.0469482857734;11.57131690531969;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2021-04-26;Training - HIIT;Bicycle;179595595;20.79;2849;125;580;42.5;48.04767407476902;11.57137356698513;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2021-04-23;Peretshofer Höhe;Racer;97011789;62.33;8786;615;748;64.4;47.87421218119562;11.542594470083714;0;0;;Tino Wendler;Rose;Unterbiberg;Deutschland
2021-04-22;Training - FTP;Bicycle;259981198;17.91;2544;105;577;35.1;48.04648635908961;11.571326376870275;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2021-04-17;Schinder;Skitour;141702346;19.93;13069;1100;1808;31.7;47.60116657242179;11.860594153404236;0;1;;Florian Seilmeier;K2-Ski;;Deutschland
2021-04-16;Peretshofer Höhe;Racer;46348811;62.58;8457;615;745;59.5;47.873973716050386;11.542615173384547;0;0;;;Rose;Unterbiberg;Deutschland
2021-04-14;Peretshofer Höhe;Racer;7004060;63.49;8900;615;748;56.4;47.87390196695924;11.542613245546818;0;0;;;Rose;Unterbiberg;Deutschland
2021-04-09;Hahnenkamm;Mountainbike;25945240;52.84;10723;1365;435;57.4;50.078234458342195;9.109565690159798;0;0;3x Hahnenkamm;;;Klein-Krotzenburg;Deutschland
2021-04-05;Hahnenkamm;Mountainbike;9035891;27.32;6143;780;435;51.8;50.07827326655388;9.109588656574488;0;0;Platten den ich nicht flicken konnte;;;Klein-Krotzenburg;Deutschland
2021-04-03;Hahnenkamm;Mountainbike;81032780;50.46;10316;1310;435;57.7;50.07829983718693;9.10967817530036;0;0;;;;Klein-Krotzenburg;Deutschland
2021-03-30;Hahnenkamm;Mountainbike;252379552;38.14;6579;605;435;51.4;50.078173941001296;9.10950475372374;0;0;Platten;;;Klein-Krotzenburg;Deutschland
2021-03-28;Hahnenkamm;Mountainbike;95367105;47.92;10166;1285;435;54.9;50.07816924713552;9.109565271064639;0;0;;;;Klein-Krotzenburg;Deutschland
2021-03-26;Glonn Runde;Racer;152457214;80.45;11617;395;662;54.8;47.942866506055;11.802159463986754;0;0;;Hanno Kaupp,Tino Wendler;Rose;;Deutschland
2021-03-24;Röthenstein;Skitour;246873316;12.42;8682;1115;1703;33.6;47.64290291815996;11.797989886254072;0;1;;Florian Seilmeier;K2-Ski;Tegernsee,Rauhenberg;Deutschland
2021-03-21;Schönberg;Skitour;89087806;13.21;9325;1210;1621;34.5;47.63864734210074;11.64257506839931;0;1;;Christian Kellner,Florian Seilmeier,Jens Repp;K2-Ski;Lenggries;Deutschland
2021-03-20;Neuhaus Schlittenfahren;Hike;168908253;3.02;1809;225;1084;24.6;47.69336641766131;11.851621745154262;0;0;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf;;;Deutschland
2021-03-13;Jasberg;Hike;71961596;8.35;6172;110;738;11.8;47.89183270186186;11.637040497735143;0;0;;Mirjam Prinz,Sonja Westermeyer,Fabian Knopf;;Otterfing;Deutschland
2021-03-12;Taubenberg;Racer;138854896;92.1;13105;950;907;52.8;47.82738550566137;11.754231322556734;0;0;;;Rose;Unterbiberg;Deutschland
2021-03-07;Waakirchen;Racer;164279490;85.68;13995;700;809;47.7;47.83725284971297;11.609130101278424;0;0;Platten ;;Rose;Unterbiberg,Kirchsee;Deutschland
2021-03-03;Unterföhring;Bicycle;208555864;26.48;4518;83;549;0.0;48.081973576918244;11.63216290064156;0;0;;;Scott;;Deutschland
2021-03-02;Crescendolauf;Running;233637157;7.81;2611;150;560;15.0;48.08077714405954;11.618290264159441;0;0;15x;;;Unterbiberg;Deutschland
2021-02-28;Neubiberg - Steigerungsintervalle;Running;1635272078561;7.78;2711;67;553;20.25;;;0;0;;;;;
2021-02-26;Locker laufen;Running;104873612;11.85;4191;254;560;17.1;48.08070011436939;11.61861078813672;0;0;28x;;;Unterbiberg;Deutschland
2021-02-24;Crescendolauf;Running;219300728;8.73;2826;166;559;16.2;48.080673879012465;11.618640376254916;0;0;17x;;;Unterbiberg;Deutschland
2021-02-17;Locker laufen;Running;211373278;8.86;3012;174;561;17.7;48.08055670000613;11.618677927181125;0;0;18x ;;;Unterbiberg;Deutschland
2021-02-16;Priener Hütte;Hike;219220014;11.85;7856;565;1229;35.9;47.69660795107484;12.304617930203676;0;0;Schlittenfahren;Mirjam Prinz;;Sachrang;Deutschland
2021-02-15;Locker laufen;Running;244279949;8.59;3068;166;561;12.8;48.08063699863851;11.618691002950072;0;0;17x;;;Unterbiberg;Deutschland
2021-02-14;Spitzstein;Skitour;230862596;9.19;8292;865;1598;27.8;47.71091393195093;12.247008690610528;0;1;;Hanno Kaupp,Florian Seilmeier;K2-Ski;Sachrang;Deutschland
2021-02-13;Locker laufen;Running;247873439;12.0;3708;75;562;13.7;48.077229084447026;11.62140179425478;0;0;;;;Unterbiberg;Deutschland
2021-02-07;Brauneck;Skitour;61919105;8.66;6591;840;1555;42.2;47.66409764997661;11.524595338851213;0;1;;Peter Müller;K2-Ski;Lenggries;Deutschland
2021-02-03;Crescendolauf;Running;64322973;6.75;2396;100;561;17.1;48.0755883269012;11.647485606372356;0;0;;;;Unterbiberg;Deutschland
2021-01-09;Taubenberg;Hike;203781304;5.49;3542;280;889;30.7;47.82746974378824;11.756919398903847;0;0;Schlittenfahrt;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf;;;Deutschland
2021-01-01;Haag;Mountainbike;180068925;49.69;10585;1020;416;56.5;50.044743809849024;9.110129624605179;0;0;;;;Rückersberg,Klein-Krotzenburg;Deutschland
2020-12-26;Hahnenkamm;Mountainbike;79390109;43.02;8890;770;445;51.2;50.07825658656657;9.10964640788734;0;0;;;;Klein-Krotzenburg;Deutschland
2020-11-29;Tierpark;Bicycle;143276570;24.24;4902;165;567;49.3;48.08541862294078;11.594212912023067;0;0;;Jonah Prinz,Mirjam Prinz;Scott,Thule;Unterbiberg;Deutschland
2020-11-21;Tierpark;Bicycle;237279595;15.3;3225;80;558;34.7;48.08544200845063;11.59409481100738;0;0;;Mirjam Prinz,Jonah Prinz;Scott;München;Deutschland
2020-11-17;Höhenkirchen;Bicycle;144352085;22.17;3077;70;575;39.0;48.01931222900748;11.707809492945671;0;0;;;Canyon-MTB;;Deutschland
2020-11-14;Spitzingscheibe;Hike;155059035;9.17;6550;550;1270;59.2;47.70063386298716;11.988038057461381;0;1;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf;;Geitau;Deutschland
2020-11-08;Sulzberg;Hike;32859062;6.78;4795;560;1118;5.88;47.74456073530018;12.062820307910442;0;1;;Mirjam Prinz,Helga Prinz,Rainer Prinz,Jonah Prinz;;Brannenburg;Deutschland
2020-10-31;Um den Breitenberg;Hike;144376258;11.78;10318;525;1038;10.9;47.773865377530456;12.409899244084954;0;0;;Mirjam Prinz,Jonah Prinz,Katrin Izaak;;Rottau;Deutschland
2020-10-30;Münster;Mountainbike;36252051;64.6;10213;700;649;50.2;47.962660705670714;11.794076543301344;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2020-10-25;Schwarzenberg;Hike;267826982;7.41;7020;490;1196;6.72;47.75306526571512;11.96446605026722;0;1;;Mirjam Prinz,Jonah Prinz;;Hundhamm;Deutschland
2020-10-23;Unterföhring;Bicycle;265693170;35.72;5119;175;558;37.5;48.08535802178085;11.593997245654464;0;0;hin direkt zurück Isarradweg;;Canyon-MTB;Unterbiberg;Deutschland
2020-10-18;Münster;Mountainbike;77453258;65.79;10235;645;642;47.4;47.95983851887286;11.834592064842582;0;0;;;Canyon-MTB;Unterbiberg;Deutschland
2020-10-13;München;Bicycle;125980627;23.5;3881;120;557;56.1;48.08538786135614;11.594013925641775;0;0;;;Scott;Unterbiberg;Deutschland
2020-09-22;Juifen;Mountainbike;73526346;46.08;14811;1375;1987;52.6;47.5433555804193;11.625489899888635;0;1;;Steffen Prinz,Tom Vaitl;Canyon-MTB;Sylvensteinspeicher;Österreich,Deutschland
2020-09-19;Neureuther Hütte;Hike;72318749;11.45;10103;590;1278;13.8;47.7253036480397;11.792803164571524;0;0;;Jonah Prinz,Sonja Westermeyer;;Tegernsee;Deutschland
2020-09-11;Taubenberg;Mountainbike;230252508;95.85;14886;900;895;65.0;47.8278517909348;11.748394332826138;0;0;;Tino Wendler;Canyon-MTB;Unterbiberg;Deutschland
2020-09-03;Neu-Esting;Bicycle;58208414;91.4;14122;450;583;37.3;48.06832398287952;11.510454146191478;0;0;hin und zurück Germering;;Canyon-MTB;Unterbiberg;Deutschland
2020-09-01;Egmating;Bicycle;240134552;18.77;2522;110;619;45.2;48.00482268445194;11.799906743690372;0;0;;;;Unterbiberg;Deutschland
2020-08-27;Rückersbacher Schlucht;Bicycle;240931594;34.71;6629;315;355;59.2;50.03585807047784;9.095081929117441;0;0;;Peter Appel;;Klein-Krotzenburg;Deutschland
2020-08-24;Hahnenkamm;Mountainbike;98491427;63.36;15412;1400;445;51.8;50.078248707577586;9.109612125903368;0;0;;;;Klein-Krotzenburg;Deutschland
2020-08-20;Matzendorf;Hike;157100559;10.52;9237;360;894;6.72;46.788807017728686;13.636737205088139;0;0;;Jonah Prinz,Mirjam Prinz;;Döbriach;Österreich
2020-08-19;Millstätter See Panorama Weg;Hike;220589444;7.89;5410;260;796;7.66;46.764141507446766;13.654478592798114;0;0;;Mirjam Prinz,Jonah Prinz;;Döbriach;Österreich
2020-08-17;Vintgar Klamm;Hike;267109297;6.6;5363;160;666;5.81;46.39501892030239;14.10756186582148;0;0;;Mirjam Prinz,Jonah Prinz;;Bled;Slowenien
2020-08-16;Peričnik Wasserfall;Hike;203626363;10.5;0;310;856;0.0;46.43923516919695;13.893589586123056;0;0;;Mirjam Prinz,Jonah Prinz;;;Slowenien
2020-08-15;Dovžan Schlucht;Hike;220944210;1.82;1295;100;722;7.39;46.387988263741136;14.330498948693275;0;0;;Mirjam Prinz,Jonah Prinz;;Trziç;Slowenien
2020-08-15;Dobrça;Climb;101561939;15.5;10430;1240;1634;20.6;46.38068921864033;14.252693932503462;0;1;;;;Glamping Slibar;Slowenien
2020-08-13;Burg Bled;Hike;120157418;3.85;2610;150;582;0.0;46.36993858963251;14.100857013836503;0;0;;Mirjam Prinz,Jonah Prinz;;Bled;Slowenien
2020-08-10;Burgruine Wolkenstein;Hike;219981972;2.69;2026;150;767;8.3;47.55651022307575;14.151299055665731;0;0;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer;;Wörschach;Österreich
2020-08-09;Reidling;Hike;100886094;6.98;5170;430;1912;9.84;47.59836592711508;14.147624680772424;0;1;;Sonja Westermeyer;;Schneehitz;Österreich
2020-08-08;Kleinmölbing;Climb;171350592;13.62;9521;810;2160;7.56;47.622250244021416;14.164542378857732;0;1;;Jonah Prinz,Mirjam Prinz,Sonja Westermeyer;;Schneehitz;Österreich
2020-08-07;Aufstieg Schneehitz;Hike;243028818;4.86;5116;530;1604;6.58;47.591450437903404;14.139436231926084;0;0;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer;;Wörschach;Österreich
2020-07-25;Samnaun Runde Tag 2;Mountainbike;214027898;71.4;17962;1675;2650;68.8;46.89607668668032;10.278186136856675;0;0;;Christian Kellner;Canyon-MTB;Heidelberger Hütte,ac_id:35332048;Schweiz,Österreich
2020-07-24;Samnaun Runde Tag 1;Mountainbike;35332048;73.73;22686;2650;2264;61.0;46.90990414470434;10.259552747011185;0;0;;Christian Kellner;Canyon-MTB;Ladis,Heidelberger Hütte;Österreich
2020-07-18;Wendelstein;Hike;161229697;8.55;5679;670;1766;12.8;47.703464264050126;12.012191852554679;0;1;;Mirjam Prinz,Jonah Prinz;;;Deutschland
2020-07-17;Aufstieg KR- Dörfler-Hütte;Hike;45478937;4.69;4551;600;1167;5.37;47.7095837239176;12.036568941548467;0;0;Tino 40er;Tino Wendler,Jonah Prinz,Mirjam Prinz,Markus Thomann;;St. Margarethen;Deutschland
2020-07-06;Egmating;Bicycle;228919505;55.75;10698;265;608;46.2;47.998839430511;11.798011092469096;0;0;;Jonah Prinz;Scott;Unterbiberg;Deutschland
2020-07-04;Deininger Weiher;Bicycle;177990133;39.6;9454;205;658;42.3;47.9790096078068;11.523322630673647;0;0;;Mirjam Prinz,Jonah Prinz,Katrin Izaak;Scott;Unterbiberg;Deutschland
2020-07-01;Taubenberg;Mountainbike;101057311;72.33;11756;785;900;63.3;47.82786402851343;11.748469602316618;0;0;1h 24.3km 2h 21.4km 3h 20km;;Canyon-MTB;Unterbiberg,Deininger Weiher;Deutschland
2020-06-26;Breitachklamm;Hike;174602022;6.0;6050;210;976;0.0;47.39619283005595;10.228349268436432;1;0;;Mirjam Prinz,Jonah Prinz;;;Deutschland
2020-06-25;Besler;Climb;261597023;27.55;20439;1400;1679;0.0;47.42519337683916;10.187966264784336;0;1;;;;Obermaiselstein;Deutschland
2020-06-24;Riedberger Horn;Hike;92317527;7.55;8629;535;1787;14.5;47.45155186392367;10.159365870058537;0;1;;Mirjam Prinz,Jonah Prinz,Lene Päßler;;Obermaiselstein;Deutschland
2020-06-23;Obermaiselstein;Hike;20233641;4.34;4316;100;845;9.81;47.443353021517396;10.249631004408002;0;0;;Mirjam Prinz,Jonah Prinz,Lene Päßler;;;Deutschland
2020-06-22;Judenkirche;Hike;246057752;16.45;14842;515;1039;41.5;47.42780123837292;10.254014655947685;0;0;;Mirjam Prinz,Jonah Prinz;;Obermaiselstein;Deutschland
2020-06-21;Obermaiselstein;Hike;162268908;9.59;9459;250;890;62.7;47.43665889836848;10.23683418519795;0;0;;Mirjam Prinz,Jonah Prinz;;;Deutschland
2020-06-13;Taubenberg;Mountainbike;40285843;99.17;16407;700;894;71.6;47.827499667182565;11.754410611465573;0;0;;;Canyon-MTB;Unterbiberg,Egmating;Deutschland
2020-06-03;Peretshofer Höhe;Mountainbike;77971711;85.81;13327;725;730;58.7;47.85648244433105;11.52563301846385;0;0;über Deininger Weiher und Isar-Radweg zurück;;Canyon-MTB;Unterbiberg,Deininger Weiher;Deutschland
2020-06-01;Heiglkopf;Hike;95341752;9.25;10742;575;1218;0.0;47.72769886069;11.513341879472136;0;1;;Mirjam Prinz,Jonah Prinz,Sonja Westermeyer;;Wackersberg;Deutschland
2020-05-31;Egmating;Bicycle;240764075;33.7;6785;82;595;25.7;48.00868330523372;11.792005710303783;0;0;;;Scott;Unterbiberg;Deutschland
2020-05-30;Perlacher Forst;Bicycle;69096856;29.05;6653;100;573;28.9;48.04123451001942;11.572979288175702;0;0;;;Scott;Unterbiberg;Deutschland
2020-05-26;Taubenberg;Mountainbike;91088843;102.96;15928;1030;896;65.4;47.82779957167804;11.748305149376392;0;0;1h 25.2, 2h 45.2 Deininger Weiher und um den Schindelberg;;Canyon-MTB;Unterbiberg,Deininger Weiher;Deutschland
2020-05-22;Hahnenkamm;Hike;193028122;5.38;7566;290;436;0.0;50.07811266928911;9.10929361358285;0;0;;Mirjam Prinz,Jonah Prinz,Peter Appel,Christa Appel;;Alzenau;Deutschland
2020-05-20;Engländer;Mountainbike;107376077;82.67;15501;1505;522;59.9;50.06808590143919;9.320806665346026;0;0;;;;Klein-Krotzenburg,Hahnenkamm,Rückersbacher Schlucht;Deutschland
2020-05-19;Hahnenkamm;Mountainbike;82379564;49.23;10940;825;436;54.3;50.07824317552149;9.109679432585835;0;0;;Berni;;Klein-Krotzenburg;Deutschland
2020-05-17;Rückersbacher Schlucht;Bicycle;86256699;37.77;7430;330;357;53.1;50.04017600789666;9.104003794491291;0;0;;Peter Appel;;Klein-Krotzenburg;Deutschland
2020-05-09;Piusheim;Mountainbike;207404714;23.06;4799;290;626;48.1;48.00252428278327;11.802833369001746;0;0;;Peter Müller;Canyon-MTB;Egmating;Deutschland
2020-05-08;Egmating;Bicycle;251409489;34.6;5168;57;600;38.8;48.008524384349585;11.791889453306794;0;0;Rückfahrt am 10.5.20;;Canyon-MTB;;Deutschland
2020-05-06;Taubenberg;Mountainbike;137423557;94.53;14062;840;918;63.4;47.82787970267236;11.748344041407108;0;0;1h: 23.7km 2h: 46.1km 3h: 68.9km;;Canyon-MTB;Unterbiberg,Deininger Weiher;Deutschland
2020-05-03;Egmating Rundtour;Bicycle;236204801;69.6;13471;240;668;38.6;47.90937594138086;11.683249427005649;0;0;Inklusive Hinweg von Unterbiberg am 1. Mai;Mirjam Prinz,Peter Müller;Scott;Egmating;Deutschland
2020-04-27;Taubenberg;Mountainbike;116587505;91.1;14197;680;884;69.4;47.8274684026837;11.7542306520045;0;0;;;Canyon-MTB;Unterbiberg,Egmating;Deutschland
2020-04-25;Spessart;Bicycle;255669251;32.26;6872;275;322;55.1;50.0368726998568;9.09182656556368;0;0;;Peter Appel;;Klein-Krotzenburg;Deutschland
2020-04-24;Hahnenkamm;Mountainbike;260998570;56.78;10646;1200;436;64.8;50.07822079584;9.1095672827214;0;0;;;;Klein-Krotzenburg,Rückersbacher Schlucht;Deutschland
2020-04-21;Hahnenkamm;Mountainbike;177711477;50.25;9373;980;436;56.3;50.0781609490514;9.1094982996583;0;0;;;;Klein-Krotzenburg,Rückersbacher Schlucht;Deutschland
2020-04-19;Hahnenkamm;Mountainbike;163575232;53.41;9911;1050;442;56.6;50.0782034453005;9.109568875283;0;0;;;;Klein-Krotzenburg,Rückersbacher Schlucht;Deutschland
2020-04-16;Hahnenkamm;Mountainbike;216668231;53.03;9943;1050;437;64.2;50.0781951472163;9.10959301516414;0;0;;;;Rückersbacher Schlucht;Deutschland
2020-04-13;Hahnenkamm;Mountainbike;32709318;56.52;10881;1090;435;60.3;50.078220628202;9.10955973900855;0;0;;;;Klein-Krotzenburg,Rückersbacher Schlucht;Deutschland
2020-04-11;Hahnenkamm;Mountainbike;3261961;55.33;10484;1030;437;63.0;50.0781793054193;9.10958865657449;0;0;;;;Klein-Krotzenburg,Rückersbacher Schlucht;Deutschland
2020-04-09;Hahnenkamm;Mountainbike;144731809;48.55;8403;880;434;60.5;50.0782174430788;9.10954657942057;0;0;;;;Klein-Krotzenburg;Deutschland
2020-04-07;Hahnenkamm;Mountainbike;49520117;48.62;8665;880;436;55.6;50.0782171078026;9.10959561355412;0;0;;;;Klein-Krotzenburg;Deutschland
2020-04-05;Hahnenkamm;Mountainbike;152483938;49.75;10006;945;445;56.1;50.0782208796591;9.10950743593276;0;0;;;;Klein-Krotzenburg;Deutschland
2020-04-01;Arbeit;Bicycle;188775964;26.58;4160;70;551;36.1;48.0866525229067;11.6331408172846;0;0;direkt;;Scott;Unterbiberg;Deutschland
2020-03-25;Arbeit;Bicycle;156732362;45.72;7990;255;586;32.2;48.0670709721744;11.553216939792;0;0;hin direkt zurück Isar-Radweg Großhesseloher Brücke (4x);;Scott;Unterbiberg;Deutschland
2020-03-18;Arbeit;Bicycle;100696514;46.23;7847;180;579;35.5;48.0425401590765;11.5219007246196;0;0;Hin direkt zurück über Grünwald;;Scott;Unterbiberg;Deutschland
2020-03-11;Arbeit;Bicycle;197120160;40.36;6236;155;536;38.4;48.0822312366217;11.6148073319346;0;0;Kita-direkt und Isar-Radweg zurück;;Scott;Unterbiberg;Deutschland
2020-03-10;Arbeit;Bicycle;132619510;26.34;4410;85;544;35.0;48.0834461934865;11.6328413318843;0;0;Direkt;;Scott;Unterbiberg;Deutschland
2020-03-07;Stubnerkogel;Skitour;222119089;7.01;9452;1100;2257;33.7;47.1135315019637;13.098927391693;0;1;Skigebiet;Christian Kellner;K2-Ski;Bad Hofgastein;Österreich
2020-03-04;Arbeit;Bicycle;180514614;37.53;5849;135;554;36.1;48.085236819461;11.5936150308698;0;0;Hinzu direkt zurück Isar-Radweg;;Scott;Unterbiberg;Deutschland
2020-02-16;Rotlahner;Skitour;238686957;17.06;16644;1315;2743;36.0;46.8411444593221;12.3066776990891;0;1;;Winfried Floßdorf,ZHS;K2-Ski;Valle di Casies,Sankt Margarethen;Italien
2020-02-15;Cima Lavinores;Skitour;181017096;17.59;14727;1085;2463;41.6;46.6250503621995;12.0747498515993;0;1;;Winfried Floßdorf,ZHS;K2-Ski;Cortina d'Ampezzo;Italien
2020-02-14;Col Duro;Skitour;53574071;13.9;13309;850;2335;29.4;46.4611108135432;12.109526284039;0;1;;ZHS,Winfried Floßdorf;K2-Ski;Val di Zoldo;Italien
2020-02-13;Forcella Val d'Arcia;Skitour;125166581;12.59;16362;1015;2497;19.9;46.4288050308824;12.1451131626964;0;1;;Winfried Floßdorf,ZHS;K2-Ski;Val di Zoldo;Italien
2020-02-12;Monte dello Grava;Skitour;260200753;5.23;4308;535;1855;55.6;46.3829509913921;12.1147284284234;0;1;;ZHS;K2-Ski;Val di Zoldo;Italien
2020-02-09;Großer Traithen;Skitour;241444161;8.0;10035;865;1862;39.3;47.6465684920549;12.0376499556005;0;1;;Christian Kellner;K2-Ski;;Deutschland
2020-01-26;Jägerkamp;Skitour;4351758;13.88;14277;1450;1746;59.1;47.6734831184149;11.906074276194;0;1;;Christian Kellner;K2-Ski;Spitzingsee,Taubenstein,Rauhkopf;Deutschland
2020-01-02;Isar-Radweg;Bicycle;210479206;28.68;6276;125;570;41.8;48.0764305405319;11.622356493026;0;0;;Mirjam Prinz,Jonah Prinz;Scott;Unterbiberg;Deutschland
2023-04-19;Ebertshausen;Racer;1690297521957;38.26;4998;325;686;57.1;47.96610231511295;11.538899727165699;0;0;;;Rose;Unterbiberg;Deutschland
2023-04-14;Hahnenkamm;Mountainbike;1681913877612;45.33;9700;1150;444;56.1;50.07819229736924;9.10959267988801;0;0;;;Peter's Bike;Klein-Krotzenburg;Deutschland
2023-04-22;Münster;Racer;1690297492482;45.32;6263;540;650;55.0;47.96264980919659;11.794073525816202;0;0;;;Rose;Egmating;Deutschland
2023-04-26;Eng;Mountainbike;1682535761795;26.49;4969;320;1212;48.2;47.40204700268805;11.566774668172002;0;0;Mautstraße gesperrt ;Florian Seilmeier;Canyon-MTB;ac_id:1682535858553;Österreich
2023-04-26;Gumpenjöchl;Skitour;1682535858553;13.8;26147;1285;2015;31.3;47.41135099902749;11.540345940738916;0;0;Schlechte Bedingungen und nicht empfehlenswert ;Florian Seilmeier;K2-Ski;Eng,Hohljoch;Österreich
2023-04-28;Ebertshausen;Racer;1690297466515;38.24;4972;325;682;54.9;47.96599259600043;11.538897966966033;0;0;;;Rose;Unterbiberg;Deutschland
2023-05-05;Aitrang ;Racer;1690297349993;156.56;24039;1760;844;64.0;47.87148680537939;10.571569511666894;0;0;;Tino Wendler;Rose;Unterbiberg,Glonn;Deutschland
2023-05-09;Münster;Racer;1690297320779;35.24;4796;415;653;54.8;47.96269775368273;11.793477656319737;0;0;;;Rose;Egmating,Unterbiberg;Deutschland
2023-05-12;Isarrunde;Racer;1690297292255;48.63;7225;500;684;62.8;47.966295098885894;11.53889536857605;0;0;;;Rose;Ebertshausen;Deutschland
2023-05-15;Grünwald;Racer;1690297277548;24.79;3640;125;601;38.7;48.03944455459714;11.52640264481306;0;0;;;Rose;;
2023-05-18;Josefsthaler Wasserfälle;Hike;1684413753147;6.87;9775;405;1123;19.1;47.67242859117687;11.886714762076735;0;0;;Mirjam Prinz,Jonah Prinz,Joshua Prinz;;;Deutschland
2023-05-19;Münster;Racer;1690297249694;35.21;5036;400;653;54.6;47.96280403621495;11.79306979291141;0;0;;;Rose;Egmating ;Deutschland
2023-05-20;Münster;Racer;1690297238717;35.22;4803;400;656;55.1;47.962697837501764;11.793945450335741;0;0;;;Rose;;Deutschland
2023-05-21;Schleifmühlklamm;Hike;1684689007375;4.65;8048;160;971;6.05;47.60470423847437;11.015798728913069;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Chrissi Wolter,Patrick Wolter;;;Deutschland
2023-05-25;Ebertshausen;Racer;1690297222638;38.3;4924;325;680;60.3;47.96625528484583;11.538912048563361;0;0;;;Rose;;Deutschland
2023-05-27;Münster;Racer;1690297206482;35.25;4744;400;652;55.1;47.96268878504634;11.794014601036906;0;0;;;Rose;Egmating ;Deutschland
2023-05-28;Wolfsklamm;Hike;1685289733596;7.86;11990;405;907;8.56;47.377228857949376;11.69107535853982;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Österreich
2023-05-29;Loderstein;Climb;1685342544970;11.07;6847;860;1806;16.4;47.37070379778743;11.908246530219913;0;1;zusätzlich kleiner Spaziergang um die Hütte;;;;Österreich
2023-06-01;Fitnesstest (3/20 min);IndoorTrainer;1690297156585;36.92;3586;0;0;59.4;;;0;0;;;Rose;;
2023-06-03;Münster;Racer;1690297135960;38.82;5408;520;651;54.8;47.96268937177956;11.793960202485323;0;0;;;Rose;;Deutschland
2023-06-05;Glonn;Racer;1690297116582;55.5;7216;375;654;56.3;47.96247781254351;11.794075537472963;0;0;;;Rose;;Deutschland
2023-06-11;Hahnenkamm;Mountainbike;1686469523794;44.96;9521;1150;427;54.1;50.07820361293852;9.109578430652618;0;0;;;Peter's Bike;;Deutschland
2023-06-14;Ebertshausen;Racer;1690297100900;38.29;4768;300;677;58.4;47.966020088642836;11.538922525942326;0;0;;;Rose;;Deutschland
2023-06-15;Unterföhring;Bicycle;1686862640133;37.65;6307;145;559;37.7;48.085423819720745;11.594341909512877;0;0;;;Scott;;Deutschland
2022-11-14;Ochsenkamp;Hike;1686862691054;17.29;14276;908;1515;10.2;47.66612330451608;11.666062669828534;0;0;;Joshua Prinz;;;Deutschland
2023-06-16;Unterföhring;Racer;1690297075330;28.46;4338;135;545;36.3;48.082359144464135;11.632536733523011;0;0;;;Rose;;Deutschland
2023-06-17;Münster;Racer;1690297057245;52.29;7321;425;649;51.6;47.96261846087873;11.79419363848865;0;0;;;Rose;;Deutschland
2023-06-19;Egmating;Racer;1690297040424;60.87;7933;195;584;44.1;48.00866235047579;11.791972182691097;0;0;;;Rose;;Deutschland
2023-06-20;Egmating;Racer;1690297018878;34.32;4524;90;593;41.3;48.01488356664777;11.744112689048052;0;0;;;;;
2023-06-22;Egmating;Racer;1690296402938;34.64;4674;90;588;36.9;48.00889746285975;11.791925244033337;0;0;;;;;
2023-06-21;Egmating;Racer;1690296994868;35.95;4827;90;584;40.5;48.008928056806326;11.791655514389277;0;0;;;;;
2023-06-24;Hohe Munde;Climb;1687615273664;13.56;21505;1660;2662;0.0;47.34705861657858;11.071768216788769;0;1;;TNG;;;Österreich
2023-06-29;Münster;Racer;1690296971648;35.81;4983;400;652;53.0;47.962669003754854;11.793872695416212;0;0;;;Rose;;Deutschland
2023-07-01;Münster;Racer;1690296947824;55.81;7539;545;649;58.0;47.962622065097094;11.793655436486006;0;0;;;Rose;;Deutschland
2023-07-06;Egmating;Racer;1688748080135;40.87;5370;160;594;38.0;48.00895764492452;11.791532970964909;0;0;;;Rose;;Deutschland
2023-07-05;Unterföhring;Racer;1690296931704;24.35;3412;100;545;41.7;48.08553596958518;11.633089687675238;0;0;;;;;
2023-07-07;Kloster Schäftlarn;Racer;1688748082119;72.79;10753;530;676;52.1;47.97599824145436;11.492459122091532;0;0;;;Rose;;Deutschland
2023-07-11;Oberlaus;Racer;1689101226037;36.9;5413;455;658;55.1;47.96258392743766;11.793998256325722;0;0;;;Rose;;Deutschland
2023-07-13;Ebertshausen;Racer;1689253024199;38.4;4715;300;677;61.7;47.966023525223136;11.53892076574266;0;0;;;Rose;;Deutschland
2023-07-22;Münster;Racer;1690127163472;35.21;5288;400;644;51.0;47.962706638500094;11.793918460607529;0;0;;;Rose;;Deutschland
2023-01-07;Villar-d'Arêne Indoor Cycling;IndoorTrainer;1690139033254;4.71;1661;324;2380;30.6;45.05303397774696;6.39473095536232;0;0;;;Rose;;
2023-07-24;Ebertshausen;Racer;1690215462363;38.49;4862;300;684;59.5;47.966048922389746;11.538916574791074;0;0;;;Rose;;Deutschland
2023-07-28;Blaueishütte;Hike;1690636814823;5.33;5390;880;1680;6.58;47.586698988452554;12.869581924751401;0;0;;Hanno Kaupp,Florian Seilmeier;;;Deutschland
2023-07-29;Hochkalter;Climb;1690636819393;14.61;22007;1040;2607;8.1;47.56938876584172;12.865741588175297;0;1;;Hanno Kaupp,Florian Seilmeier;;ac_id:1690636814823;Deutschland
2023-08-03;Ebertshausen;Racer;1691078882718;38.4;4700;300;676;60.8;47.96598312444985;11.538946582004428;0;0;;;Rose;;Deutschland
2023-08-08;Perlacher Forst;Racer;1691494447877;29.51;3673;155;578;46.2;48.05557133629918;11.566022140905261;0;0;;;Rose;;Deutschland
2023-08-10;Kloster Schäftlarn;Racer;1691669000031;57.69;8000;525;678;63.4;47.9660287220031;11.538941469043493;0;0;;;Rose;;Deutschland
2023-08-13;Hahnenkamm;Mountainbike;1691922193634;50.46;11425;1425;426;58.0;50.078303860500455;9.10967230796814;0;0;;;Peter's Bike;Klein-Krotzenburg;Deutschland
2023-08-14;Alzenau;Bicycle;1692166235439;24.16;4424;85;135;36.5;50.087281968444586;9.073329884558916;0;0;;;Peter's Bike;Klein-Krotzenburg;Deutschland
2023-08-16;Hahnenkamm;Mountainbike;1692200932853;54.74;11702;1415;426;63.9;50.078258849680424;9.109615981578827;0;0;;;Peter's MTB bike;Klein-Krotzenburg;Deutschland
2023-08-19;Tierpark;Racer;1692439491847;27.91;4045;150;571;54.7;48.04587012156844;11.570661691948771;0;0;;;Rose;;Deutschland
2023-08-22;Köpperl;Hike;1692728412034;2.47;0;315;1728;0.0;47.59157876484096;14.143825080245733;0;1;;Jonah Prinz,Joshua Prinz,Sonja Westermeyer,Fabian Knopf,Mirjam Prinz;;Schneehitz;Österreich
2023-08-23;Kleinmölbing;Climb;1692780085290;11.5;9388;760;2160;8.67;47.62220246717334;14.164528883993626;0;1;;;;Schneehitz;Österreich
2023-08-24;Wörschachklamm und Spechtensee;Hike;1692899910667;13.7;17805;500;1082;0.0;47.56390674971044;14.10051193088293;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Sonja Westermeyer,Fabian Knopf;;Wörschach;Österreich
2023-08-25;Spechtensee;Mountainbike;1692969685968;20.66;5912;700;1140;45.4;47.55796833895147;14.132225029170513;0;0;;;Canyon-MTB;Wörschach,ac_id:1692969713806;Österreich
2023-08-25;Aicherlstein;Hike;1692969713806;0.68;827;40;1180;11.2;47.55696326494217;14.135724725201726;0;1;;;;;Österreich
2023-08-27;Hirschenstein;Mountainbike;1693141817932;15.01;4027;510;1077;59.1;48.96458538249135;12.879575081169605;0;1;;;Canyon-MTB;;Deutschland
2023-08-29;Hirschenstein;Mountainbike;1693313756228;14.95;3707;525;1095;55.5;48.964437106624246;12.87973995320499;0;1;;;Canyon-MTB;Achslach;Deutschland
2023-08-30;Großer Pfahl;Hike;1693406493041;7.17;12006;190;564;7.79;49.08796917647123;12.851566364988685;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2023-08-31;Hirschenstein;Mountainbike;1693482702611;15.29;3624;525;1095;58.1;48.96451262757182;12.879686979576945;0;1;;;Canyon-MTB;Achslach;Deutschland
2023-09-03;Hirschenstein;Hike;1693747398513;8.21;14008;515;1056;6.45;48.96455369889736;12.879797033965588;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;Achslach;Deutschland
2023-09-04;Hirschenstein;Mountainbike;1693833017349;31.43;7400;950;1095;57.9;48.964541126042604;12.879760321229696;0;1;;;Canyon-MTB;Achslach;Deutschland
2023-09-05;Hirschenstein;Mountainbike;1693930112313;15.45;4356;505;1095;65.1;48.96453073248267;12.879634676501155;0;1;;;Canyon-MTB;Achslach;Deutschland
2023-09-07;Großer Arber;Hike;1694087625464;4.39;8543;420;1456;0.0;49.11246797069907;13.136098114773631;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2023-09-08;Klosterstein am Vogelsang ;Mountainbike;1694175493802;16.84;3770;475;1022;67.6;48.934294525533915;12.943636793643236;0;1;;;Canyon-MTB;Achslach;Deutschland
2023-09-11;Ebertshausen;Racer;1694435961128;38.3;4819;300;687;62.2;47.96624748967588;11.538924789056182;0;0;;;Rose;;Deutschland
2023-09-14;Sauerlach;Racer;1694711385216;32.35;4260;155;618;44.4;47.979248240590096;11.646584384143353;0;0;;;;;
2023-09-17;Stöttwang;Racer;1694945768247;96.85;13952;1025;797;62.0;47.874133391305804;10.77154646627605;0;0;;;Rose;Andechs;Deutschland
2023-09-21;Egmating;Bicycle;1695369556855;58.28;7711;225;594;40.8;48.0148284137249;11.744307316839695;0;0;;;Rose;;Deutschland
2023-09-24;Plumsjoch;Mountainbike;1695559201125;34.09;8582;750;1647;52.0;47.44544539600611;11.618986129760742;0;0;;Hanno Kaupp;Canyon-MTB;Hinterriß;Österreich
2023-09-24;Bettlerrkarspitze Vorgipfel;Climb;1695559225239;3.06;7597;530;2175;0.0;47.433750964701176;11.617764802649617;0;1;;Hanno Kaupp;;ac_id:1695559201125;Österreich
2023-09-29;Perlacher Forst;Racer;1696174231669;26.14;3392;100;593;39.1;48.02687857300043;11.583348205313087;0;0;;;Rose;;Deutschland
2023-10-03;Taubenberg;Hike;1696342327594;6.63;11530;255;923;13.1;47.82739588167501;11.754104363962966;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2023-10-04;Ebertshausen;Racer;1696420884666;38.33;4862;300;687;61.1;47.966263918206096;11.538891512900591;0;0;;;Rose;;Deutschland
2023-10-07;Münster;Racer;1696681176779;48.7;6911;330;655;50.5;47.96260471455753;11.793876551091671;0;0;;;Rose;;Deutschland
2023-10-11;Ebertshausen;Bicycle;1697030548810;38.72;4968;300;683;60.9;47.96603593043983;11.538916742429137;0;0;;;Rose;;Deutschland
2023-10-13;Bischof;Climb;1697218186806;13.87;16534;1530;2032;9.54;47.54041587933898;11.17578805424273;0;1;;Hanno Kaupp;;Hohe Fricken;Deutschland
2023-10-28;Egmating;Bicycle;1698565459914;19.01;2884;105;610;44.4;48.001838475465775;11.795277753844857;0;0;;;;;
2023-11-01;Taubenberg;Racer;1698865416405;74.6;11227;605;887;56.9;47.827495727688074;11.756947562098503;0;0;;;Rose;Egmating ;Deutschland
2023-11-05;Perlacher Forst;Racer;1699190940708;27.69;4811;175;594;37.2;48.03074103780091;11.577409375458956;0;0;;Joshua Prinz;Rose,Thule;;Deutschland
2023-11-09;Unterföhring;Racer;1699559689997;29.16;4494;85;546;39.4;48.08237322606146;11.632518796250224;0;0;;;Rose;;Deutschland
2023-11-11;Unterföhring;Racer;1699741284933;30.52;5054;155;549;41.2;48.08433040045202;11.627428466454148;0;0;;;;;
2023-11-18;Egmating;Racer;1700679113602;17.04;2515;60;584;31.9;48.008413994684815;11.791739836335182;0;0;;;Rose;;Deutschland
2023-11-22;Unterföhring;Racer;1700679113061;27.0;4119;125;546;35.1;48.082392839714885;11.632551737129688;0;0;;;Rose;;Deutschland
2023-11-27;Schwelle;IndoorTrainer;1701258970416;28.46;3192;0;0;38.0;;;0;0;;;Rose;;
2023-12-08;Weitlahner;Skitour;1702058126997;16.03;15028;1410;1691;42.4;47.721252758055925;12.333003329113126;0;0;;Florian Seilmeier;K2-Ski;;Deutschland
2023-11-12;München Ost West;Racer;1702746191686;23.28;3676;90;557;45.7;48.08532726019621;11.593681331723928;0;0;;;Rose;;Deutschland
2023-11-30;VO2max;IndoorTrainer;1702746365352;16.77;2079;0;0;51.83;;;0;0;;;;;
2023-12-06;Basis;IndoorTrainer;1702746365454;11.24;1382;0;0;36.04;;;0;0;;;;;
2023-11-24;Neubiberg Cyclocross;Racer;1702746365562;17.03;2496;95;593;34.8;48.00932644866407;11.788630150258541;0;0;;;;;
2023-12-13;Schwelle;IndoorTrainer;1702746365613;20.72;3027;0;0;61.24;;;0;0;;;;;
2023-11-15;Schwelle;IndoorTrainer;1702746365722;18.59;2696;0;0;77.02;;;0;0;;;;;
2023-12-16;Egmating;Racer;1702746366429;45.49;6824;125;592;38.6;48.01462473347783;11.744987005367875;0;0;;;Rose;;Deutschland
2023-12-17;Egmating;Bicycle;1702840458292;17.05;2726;70;589;28.6;48.008429668843746;11.791775124147534;0;0;;;Canyon-MTB;;Deutschland
2023-12-18;Unterföhring;Racer;1702929345201;26.67;3854;80;551;39.8;48.08198916725814;11.631306437775493;0;0;;;Rose;;Deutschland
2023-12-20;Egmating;Racer;1703088699123;34.04;5155;80;592;35.0;48.01429281011224;11.746961195021868;0;0;;;Rose;;Deutschland
2023-12-27;Hahnenkamm;Mountainbike;1704015934037;38.17;7681;575;444;56.3;50.07824418134987;9.109647581353784;0;0;;;Peter's Bike;;Deutschland
2024-01-03;Münster;Racer;1704300845275;48.9;7122;330;656;52.1;47.962757935747504;11.792907351627946;0;0;;;Rose;;Deutschland
2024-01-12;Schwelle;IndoorTrainer;1705266314458;16.96;3867;0;0;29.36;;;0;0;;;;;
2024-01-14;Sprint;IndoorTrainer;1705266314778;13.77;2851;0;0;28.45;;;0;0;;;;;
2024-01-16;Basis;IndoorTrainer;1705731174874;14.26;3386;0;0;17.87;;;0;0;;;;;
2024-01-19;Basis;IndoorTrainer;1705731174964;17.72;3997;0;0;20.29;;;0;0;;;;;
2024-01-18;Schwelle;IndoorTrainer;1705731175749;13.69;3139;0;0;27.34;;;0;0;;;;;
2024-01-22;Schwelle;IndoorTrainer;1705952214533;13.04;2892;0;0;20.32;;;0;0;;;;;
2024-01-23;Basis;IndoorTrainer;1706024616167;16.99;3918;0;0;19.79;;;0;0;;;;;
2024-01-26;Neuperlach;Bicycle;1706379244628;35.02;5850;115;593;28.3;48.008865527808666;11.792009063065052;0;0;;;Scott;;Deutschland
2024-01-27;Gerstinger Joch ;Skitour;1706379248278;21.05;23534;1490;2035;35.4;47.348364014178514;12.260999428108335;0;1;;Hanno Kaupp,Florian Seilmeier,Christian Kellner;K2-Ski;Aschau;Österreich
2024-01-28;Großer Tanzkogel ;Skitour;1706451683396;17.41;14887;1115;2097;39.4;47.33505665324628;12.256246469914913;0;1;;Hanno Kaupp,Christian Kellner,Florian Seilmeier;K2-Ski;Aschau;Österreich
2024-01-31;Münster;Mountainbike;1706717303339;19.27;3269;355;641;50.6;47.95983474701643;11.83484653942287;0;0;;;Canyon-MTB;;Deutschland
2024-02-01;Basis;IndoorTrainer;1706802988140;18.04;3894;0;0;20.69;;;0;0;;;;;
2024-02-02;Münster;Mountainbike;1706890376241;22.8;4067;485;629;50.7;47.95985151082277;11.83500093407929;0;0;;;Canyon-MTB;;Deutschland
2024-02-05;Basis;IndoorTrainer;1707161629744;18.97;3929;0;0;29.6;;;0;0;;;;;
2024-02-07;Schwelle;IndoorTrainer;1707331199537;13.32;2705;0;0;20.59;;;0;0;;;;;
2024-02-08;Kaps;Mountainbike;1707403705776;31.46;5409;485;641;57.9;47.944605499506;11.837153239175677;0;0;;;Canyon-MTB;;Deutschland
2024-02-12;Hahnenkamm;Mountainbike;1707769318078;55.72;11322;1000;444;55.8;50.07818299345672;9.109598798677325;0;0;;;Peter's Bike;;Deutschland
2024-02-15;Basis;IndoorTrainer;1708026671120;19.8;4027;0;0;32.95;;;0;0;;;;;
2024-02-16;Jakobsbaiern;Racer;1708086338875;31.68;4640;480;640;60.6;47.94459552504122;11.837132200598717;0;0;;;Rose;;Deutschland
2024-02-18;Jakobsbaiern;Racer;1708265447058;31.6;4352;480;642;59.5;47.944603404030204;11.837146785110235;0;0;;;Rose;;Deutschland
2024-02-22;VO2max;IndoorTrainer;1708621087745;20.46;3000;0;0;36.45;;;0;0;;;;;
2024-02-24;Jakobsbaiern;Mountainbike;1708781190736;30.57;4808;495;642;60.7;47.94459426775575;11.83717092499137;0;0;;;Canyon-MTB;;Deutschland
2024-02-27;Glonn;Racer;1709049689841;22.4;4135;195;631;44.9;47.998819733038545;11.79810312576592;0;0;;Jonah Prinz;Rose,Thule;;Deutschland
2024-02-28;Unterföhring;Bicycle;1709157763974;57.79;8449;165;595;39.3;48.00888338126242;11.791996657848358;0;0;;;Rose;;Deutschland
2024-03-02;Große Reibn Tag 1;Skitour;1709566524164;4.63;6243;655;1729;17.1;47.574567859992385;13.042485499754548;0;0;;Hanno Kaupp,Florian Seilmeier;K2-Ski;Stahlhaus;Deutschland
2024-03-04;Große Reibn Tag 3;Skitour;1709566525249;22.28;21446;1085;2225;43.1;47.50876690261066;12.880256697535515;0;0;;Hanno Kaupp,Florian Seilmeier;K2-Ski;ac_id:1709566527019;Deutschland
2024-03-03;Große Reibn Tag 2;Skitour;1709566527019;28.46;35452;2340;2368;40.7;47.4718857742846;12.973065488040447;0;1;;Hanno Kaupp,Florian Seilmeier;K2-Ski;ac_id:1709566524164,Windschattenkopf,Schneibstein;Deutschland,Österreich
2024-03-09;Stöttwang;Racer;1709987411634;107.07;16230;1080;753;53.0;;;0;0;;;Rose;;Deutschland
2024-03-14;München;Racer;1710459901866;52.2;7651;130;593;47.5;48.00882319919765;11.792019037529826;0;0;;;Rose;;Deutschland
2024-03-16;Huberspitz;Hike;1710614618145;7.04;12609;300;1062;8.8;47.73782939650118;11.834213621914387;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Sonja Westermeyer,Fabian Knopf,Iona Westermeyer;;;Deutschland
2024-03-17;Jakobsbaiern;Racer;1710686825652;31.63;4230;480;641;65.1;47.94455671682954;11.83721786364913;0;0;;;Rose;;Deutschland
2024-03-11;Basis;IndoorTrainer;1710942009872;23.07;3758;0;0;25.06;;;0;0;;;;;
2024-03-20;Jakobsbaiern;Racer;1710959381150;31.67;4167;480;640;65.6;47.94455629773438;11.83719095773995;0;0;;;Rose;;Deutschland
2024-03-22;Längentaler Weißer Kogel ;Skitour;1711130376024;22.043080000000003;20244;1875;3217;43.1;47.07005850970745;11.086002113297582;0;1;;Florian Seilmeier;K2-Ski;;Österreich
2024-03-25;Münster;Racer;1711387509449;19.67;3124;315;638;51.6;47.95977741479874;11.834412943571806;0;0;;;Rose;;Deutschland
2024-03-30;Gindelalmschneid;Racer;1711808007790;80.46;12410;1120;1336;63.1;47.72363665513694;11.798578798770905;0;1;;;Rose;;Deutschland
2024-04-01;Münster;Racer;1711991849379;22.78;3673;485;641;53.7;47.95984111726284;11.835075616836548;0;0;;;Rose;;Deutschland
2024-04-05;Jakobsbaiern;Racer;1712331090543;35.22;5177;605;641;65.8;47.9446710459888;11.836789883673191;0;0;;;Rose;;Deutschland
2024-04-08;Jakobsbaiern;Racer;1712591530397;31.59;4168;480;641;63.3;47.944593681022525;11.837165895849466;0;0;;;Rose;;Deutschland
2024-04-12;Schliersee;Racer;1712920166532;87.35;13157;1025;1269;62.3;47.683057179674506;11.844663340598345;0;0;;;Rose;;Deutschland
2024-04-12;Wasserspitz;Climb;1712920178970;1.98;2458;290;1552;9.04;47.68037681467831;11.835870975628495;0;1;;;;;Deutschland
2024-04-16;FTP-Testtraining;IndoorTrainer;1713361170803;10.71;1738;0;0;51.2;;;0;0;;;;;
2024-04-21;Egmating Laufen;Running;1713688359343;11.79;4012;100;628;17.8;47.99862451851368;11.799539364874363;0;0;;;;;
2024-04-07;Egmating Laufen;Running;1713688400541;11.98;4171;100;627;15.1;47.99864002503455;11.799481613561511;0;0;;;;;
2024-04-14;Egmating Laufen;Running;1713688400541;12.02;4033;100;628;15.1;47.99865435808897;11.799464765936136;0;0;;;;;
2024-04-26;Jakobsbaiern;Racer;1714201113041;51.69;7323;860;644;63.1;47.94460801407695;11.837108815088868;0;0;;;Rose;;Deutschland
2024-04-20;Ramersberg ;Racer;1714201674035;71.95;10943;890;625;55.1;48.02203458733857;11.837197160348296;0;0;;;Rose;;Deutschland
2024-04-28;Egmating - Long Run;Running;1714289485405;15.2;5011;125;626;13.3;47.997591784223914;11.797859463840723;0;0;;;;;
2024-04-29;Taubenberg;Racer;1714415063402;57.21;7555;595;900;61.6;47.8274944704026;11.754178516566753;0;0;;;Rose;;Deutschland
2024-05-02;Piusheim;Racer;1714671404241;29.43;4440;425;640;70.5;47.9446018114686;11.83715189807117;0;0;;;Rose;;Deutschland
2024-05-05;Lech;Racer;1714918284818;87.58;10759;640;643;52.1;48.08359941467643;11.008075475692749;0;0;;;Rose;;Deutschland
2024-05-09;Valepp;Racer;1715251881908;111.71;15835;1200;1136;68.8;47.672265311703086;11.886352328583598;0;0;;;Rose;;Deutschland
2024-05-12;Hochplattenbahn;Hike;1715541824344;4.47;8295;205;1210;9.47;47.77140134945512;12.409004224464297;0;0;;Mirjam Prinz,Jonah Prinz,Joshua Prinz,Peter Appel,Christa Appel;;;Deutschland
2024-05-13;Münster;Racer;1715620076161;25.72;3448;185;655;47.1;47.96269113197923;11.793834222480655;0;0;;;Rose;;Deutschland
2024-05-16;Jakobsbaiern;Racer;1715888405512;31.63;4363;480;642;61.8;47.94456861913204;11.83719439432025;0;0;;;Rose;;Deutschland
2024-05-17;Taufkirchen;Racer;1716011879209;40.44;6021;150;595;34.6;48.008934846147895;11.791935553774238;0;0;;;Rose;;Deutschland
2024-05-19;Kirchsee;Racer;1716134883793;67.15;8452;450;759;49.2;47.83678790554404;11.653253780677915;0;0;;;Rose;;Deutschland
2024-05-20;Lenggries;Racer;1716226474947;107.22;14742;800;757;60.2;47.83848322927952;11.65217368863523;0;0;;;Rose;;Deutschland
2024-05-20;Brauneck;Hike;1716226497273;4.73;8777;290;1625;8.83;47.66056635417044;11.508742896839976;0;1;;Helga Prinz,Rainer Prinz,Mirjam Prinz,Joshua Prinz,Jonah Prinz;;;Deutschland
2021-05-28;Rechelkopf;BikeAndHike;1716295108222;62.66;0;1100;1330;0.0;;;0;0;;;Canyon-MTB;Mariastein,Otterfing;Deutschland
2024-05-24;Jakobsbaiern;Racer;1716560449228;31.61;4188;480;642;65.0;47.94452561996877;11.837215516716242;0;0;;;Rose;;Deutschland
2024-05-26;Plansee;Hike;1716744484481;4.68;6431;110;1044;6.35;47.47315261512995;10.794411124661565;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Lene Päßler;;;Österreich
2024-05-27;Ruine Ehrenberg ;Hike;1716820150112;6.93;11289;240;1115;8.5;47.46190753765404;10.722727831453085;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Lene Päßler;;;Österreich
2024-05-29;Die Dietzl;Hike;1716972751271;11.21;9089;1100;1817;9.61;47.48774265870452;10.64468678086996;0;0;;;;;Österreich
2024-05-29;Hornberg;Hike;1717082326791;2.99;5914;225;1907;5.81;47.47825141064823;10.641236454248428;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Lene Päßler;;ac_id:1716972751271;Österreich
2024-06-07;Hart im Zillertal;Racer;1717779072205;88.98;16244;1770;1157;59.4;47.3667177837342;11.887174844741821;0;0;;;Rose;;Deutschland,Österreich
2024-06-08;Wiedersberger Horn ;Climb;1717861333021;12.31;13892;1015;2127;13.2;47.361270217224956;11.922877309843898;0;1;;Matthias Hechinger;;Loderstein;Österreich
2024-06-09;Kufstein;Racer;1717930052312;64.33;8587;265;1148;67.7;47.36610246822238;11.887388583272696;0;0;;;Rose;;Österreich,Deutschland
2024-06-12;Piusheim;Racer;1718222861258;29.43;3853;410;643;62.5;47.94458353891969;11.837188107892871;0;0;;;Rose;;Deutschland
2024-06-14;Piusheim;Racer;1718398851644;29.43;3754;410;641;63.1;;;0;0;;;Rose;;Deutschland
2024-06-18;Piusheim;Racer;1718726732775;29.39;3594;410;640;61.5;47.94458613730967;11.837164890021086;0;0;;;Rose;;Deutschland
2024-06-23;Vorderer Drachenkopf;Climb;1719154101025;6.01;8013;680;2303;10.1;47.35769709572196;10.926484521478415;0;1;;TNG;;;Österreich
2024-06-25;Münster;Racer;1719325392647;31.43294;3652;190;629;49.2;47.98751690424979;11.813639989122748;0;0;;;Rose;;Deutschland
2024-06-23;Seebensee;Mountainbike;1719415575466;44.0;10200;1025;1676;59.0;;;0;0;;TNG;;ac_id:1719154101025;Österreich
2024-06-27;Taubenberg;Racer;1719506573263;55.99;7053;530;884;61.2;;;0;0;;;Rose;;Deutschland
2024-07-02;Piusheim;Racer;1719926624042;29.42;4190;410;642;65.3;47.944584880024195;11.83717050589621;0;0;;;Rose;;Deutschland
2024-07-04;Feldkirchen-Westerham;Racer;1720107273542;36.06;5203;540;651;69.4;47.939441073685884;11.835375856608152;0;0;;;Rose;Münster;Deutschland
2024-07-05;Motorwelt München ;Racer;1720248244087;65.9;8745;180;595;37.3;48.00841265358031;11.791737154126167;0;0;;;Rose;;Deutschland
2024-07-08;Unterlaus;Racer;1720459217259;28.68;3939;435;642;63.5;47.944649839773774;11.836959617212415;0;0;;;Rose;;Deutschland
2024-07-10;Unterhaching ;Racer;1720629769796;40.21;5297;130;594;45.1;48.008981700986624;11.791154192760587;0;0;;;Rose;;Deutschland
2024-07-12;Münster;Racer;1720853657090;24.46;3241;240;656;49.1;47.96269406564534;11.793876802548766;0;0;;;Rose;;Deutschland
2024-07-14;Brunnenkopf;Climb;1720964613715;4.8;6000;450;1710;13.5;47.582792434841394;10.923778591677547;0;1;;;;ac_id:1721070765373;Deutschland
2024-07-14;Stöttwang;Racer;1721070765373;88.34;13443;1160;1249;54.9;47.59025023318827;10.930835818871856;0;0;;;Rose;;Deutschland
2024-07-16;Glonn;Racer;1721152473700;21.44;3979;200;632;47.6;47.987000830471516;11.814216831699014;0;0;;;Rose,Thule;;Deutschland
2024-07-17;Glonn;Racer;1721214324630;18.55;2645;185;630;55.1;47.98820707015693;11.812651008367538;0;0;;;Rose;;Deutschland
2024-07-21;Grafing;Racer;1721580557977;29.69482;3750;275;622;60.2;48.02207691594958;11.836197953671217;0;0;;;Rose;;Deutschland
2024-07-23;Münster;Racer;1721817367622;28.25;3723;240;655;56.3;47.962707644328475;11.793499197810888;0;0;;;Rose;;Deutschland
2024-07-25;Haar;Racer;1721927388156;27.51;3426;85;597;40.9;48.00891129299998;11.791913006454706;0;0;;;Rose;;Deutschland
2024-07-26;Risserkogel ;Climb;1721993261339;3.46;5392;405;1825;9.17;47.636917820200324;11.803526133298874;0;1;;;Rose;ac_id:1726745739223;Deutschland
2024-07-30;Unterhaching ;Racer;1722357486232;40.97;5436;130;595;42.3;;;0;0;;;Rose;;Deutschland
2024-08-01;Piusheim;Racer;1722521940711;29.43;4168;410;640;63.3;;;0;0;;;Rose;;Deutschland
2024-07-18;Piusheim;Racer;1722624058398;29.44;3733;410;641;65.6;47.94460960663855;11.837116023525596;0;0;;;Rose;;Deutschland
2024-08-05;Pirklalm;Hike;1722879565037;4.0;6000;160;1259;8.06;47.40071545355022;13.857080088928342;0;0;;;;;
2024-08-06;Gradenbachfall;Hike;1722974260149;6.2;7566;175;926;10.6;47.447851840406656;13.796099312603474;0;0;;Jonah Prinz,Joshua Prinz,Mirjam Prinz,Sonja Westermeyer,Fabian Knopf;;;Österreich
2024-08-07;Pleschnitzzinken;Climb;1723014260115;11.39;10200;975;2112;8.2;47.372951321303844;13.856051797047257;0;1;;;;;Österreich
2024-08-08;Gasselhöhe ;Hike;1723132807190;3.34;5516;295;2001;6.62;47.35293776728213;13.592039616778493;0;1;;Jonah Prinz;;;Österreich
2024-08-14;Kriška gora;Climb;1723636283671;13.2;12061;1060;1472;11.8;46.35195563547313;14.33403242379427;0;1;;;;;Slowenien
2024-08-13;Dovžan Schlucht;Hike;1723636313441;1.39;3014;90;705;11.0;46.3834036141634;14.329130686819553;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Slowenien
2024-08-15;Peričnik Wasserfall;Hike;1723738818869;6.83;10036;160;838;0.0;46.4394212141633;13.894287189468741;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Slowenien
2024-08-22;Gerlitzen;Hike;1724439123057;4.68;6607;200;1911;13.8;46.695048809051514;13.914448767900467;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Österreich
2024-08-23;Piusheim;Racer;1724439123056;29.43;3746;410;641;61.5;47.94455386698246;11.837197998538613;0;0;;;Rose;;Deutschland
2024-08-24;Biberg;Racer;1724521586593;44.71;6520;670;642;61.9;47.944562919437885;11.837208392098546;0;0;;;Rose;;Deutschland
2024-08-27;Hahnenkamm;Mountainbike;1724778597060;39.49;7718;870;446;56.3;50.078236386179924;9.109599217772484;0;0;;;Scott;;Deutschland
2024-08-30;Hahnenkamm;Mountainbike;1725038485993;39.53232;7518;870;445;53.5;50.0782376434654;9.109621848911047;0;0;;;Scott;;Deutschland
2024-09-05;Jakobsbaiern;Racer;1725544567528;31.62;4376;480;643;61.4;47.94447968713939;11.837288606911898;0;0;;;Rose;;Deutschland
2024-09-10;Piusheim;Racer;1725989362773;29.43643;4244;410;642;64.5;47.944575073197484;11.837196238338947;0;0;Abdeckung für Powermeter fehlt ;;Rose;;Deutschland
2024-09-12;Otterfing;Racer;1726143589456;33.40587;3940;135;674;38.7;47.915479224175215;11.686884239315987;0;0;ohne Powermeter ;;Rose;;Deutschland
2024-09-17;Otterfing;Racer;1726580550646;33.36198;3866;135;674;40.0;47.91521301493049;11.68449304997921;0;0;;;Rose;;Deutschland
2024-09-17;Glonn;Bicycle;1726688319688;21.66;4318;200;631;41.6;47.987542636692524;11.813480397686362;0;0;;Joshua Prinz,Jonah Prinz;Rose,Thule;;Deutschland
2024-07-26;Röthenstein Alm;Racer;1726745739223;107.54;16245;1335;1429;59.3;47.64382149092853;11.792358923703432;0;0;;;Rose;;Deutschland
2024-09-20;Hochgern;Climb;1726829987823;15.65;14263;1145;1748;9.31;47.751056123524904;12.516673542559147;0;1;;Hanno Kaupp;;;Deutschland
2024-09-21;Kupferbach ;BikeAndHike;1726932172900;17.61;7856;225;632;0.0;47.99882593564689;11.798094240948558;0;0;;Sonja Westermeyer,Fabian Knopf,Mirjam Prinz,Joshua Prinz,Jonah Prinz;Rose,Thule;;Deutschland
2024-09-25;Unterföhring;Racer;1727284438855;56.09;7665;190;598;51.7;48.008946580812335;11.791827846318483;0;0;;;Rose;;Deutschland
2024-09-20;Hofoldinger Forst ;Racer;1727594203425;20.77;2829;70;629;36.2;47.97792607918382;11.685342472046614;0;0;;;Rose;;Deutschland
2024-10-01;Piusheim;Racer;1727795834434;29.41;3963;410;643;63.4;47.944502737373114;11.837234375998378;0;0;;;Rose;;Deutschland
2024-10-08;Piusheim;Racer;1728386789200;29.4;3789;410;640;65.9;47.9445449821651;11.83722272515297;0;0;;;Rose;;Deutschland
2024-10-16;Unterföhring;Racer;1729099709768;54.29;7807;200;597;44.7;48.00890760496259;11.791954329237342;0;0;;;Rose;;Deutschland
2024-10-18;Bayreuther Hütte ;Mountainbike;1729256037856;16.09;6829;960;1560;41.1;47.44511330500245;11.820999635383487;0;0;;;Canyon-MTB;;Österreich
2024-10-18;Rofanspitze ;Climb;1729256100098;9.46;10895;870;2259;9.14;47.45764802210033;11.793191246688366;0;1;;;;Sagzahn,Vorderes Sonnwendjoch,ac_id:1729256037856;Österreich
2024-10-22;Piusheim;Racer;1729606618284;29.41;3949;410;641;68.8;47.94457876123488;11.837159022688866;0;0;;;Rose;;Deutschland
2024-10-25;Lichtensteinklamm;Hike;1729947597911;3.99;4078;130;682;30.7;;;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Österreich
2024-10-26;Fulseck;Climb;1729947597833;10.32;11215;1435;2035;11.3;;;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Helga Prinz,Rainer Prinz,Steffen Prinz;;;Österreich
2024-10-29;Piusheim;Racer;1730206089079;29.42;4004;410;643;64.4;;;0;0;;;Rose;;Deutschland
2024-10-31;Otterfing;Racer;1730383232654;33.41;3767;135;674;42.7;47.91515115648508;11.684188954532146;0;0;;;Rose;;Deutschland
2024-11-01;Kirchsee;Racer;1730480936210;66.63;8632;450;761;49.0;47.83682352863252;11.65326870046556;0;0;;;Rose;;Deutschland
2024-11-06;Taubenberg;Racer;1730893819007;56.48;8384;570;900;56.5;47.82745063304901;11.754226125776768;0;0;;;Rose;;Deutschland
2024-11-15;Soinwand;Climb;1731683667033;15.35;18726;1295;1838;8.36;47.70346191711724;12.012168550863862;0;0;;Hanno Kaupp,Christian Kellner;;;Deutschland
2024-11-17;Jakobsbaiern;Mountainbike;1731859689947;30.16;5042;550;645;59.0;47.9446158092469;11.83715927414596;0;0;;;Canyon-MTB;;Deutschland
2024-11-22;Indoor Cycling;IndoorTrainer;1732444195692;31.32;4169;0;0;35.57;;;0;0;;;;;
2024-11-24;Indoor Cycling;IndoorTrainer;1732444195687;13.02;2129;0;0;43.1;;;0;0;;;;;
2024-11-20;Indoor Cycling;IndoorTrainer;1732444207506;9.34;1631;0;0;41.62;;;0;0;;;;;
2024-11-27;Basis;IndoorTrainer;1732786929039;14.87;2346;0;0;41.45;;;0;0;;;;;
2024-11-25;Poing ;Mountainbike;1732786957221;45.67;7321;200;595;43.3;48.00844643265009;11.791729023680091;0;0;;;Canyon-MTB;;Deutschland
2024-11-28;Schwelle;IndoorTrainer;1732823249529;17.8;2704;0;0;27.38;;;0;0;;;;;
2024-12-01;Piusheim;Racer;1733067803674;29.43;4489;410;643;52.9;47.94462410733104;11.837095655500889;0;0;;;Rose;;Deutschland
2024-12-03;München;Racer;1733244451934;54.69;8320;150;593;34.6;48.00899846479297;11.791474130004644;0;0;;;Rose;;Deutschland
2024-12-10;Indoor Cycling;IndoorTrainer;1733906859733;20.63;2873;0;0;31.54;;;0;0;;;;;
2024-12-11;Sprint;IndoorTrainer;1733949786971;20.03;3078;0;0;50.25;;;0;0;;;;;
2024-12-18;Unterföhring;Racer;1734529687744;47.14;7607;175;596;31.3;48.00887994468212;11.792000010609627;0;0;;;;;
2024-12-27;Haag;Bicycle;1735312910528;38.29;7383;620;406;58.8;50.044739451259375;9.110212102532387;0;0;;;Peter's Bike;;Deutschland
2025-01-01;Egmating;Running;1735758363566;9.56;3298;70;628;19.3;47.9976269043982;11.798105975612998;0;0;;Tino Wendler;;;Deutschland
2024-09-07;Gindelalm;Racer;1736419221126;79.51;11543;925;1251;64.7;47.72535896860063;11.801706422120333;0;0;;;Rose;;Deutschland
2024-09-07;Gindelalmschneid;Hike;1736419299164;1.32;1123;90;1335;8.6;47.72368527017534;11.795847555622458;0;1;;;;ac_id:1736419221126;Deutschland
2025-01-10;Seekarlspitze;Skitour;1736533103313;11.85;16359;1025;2261;39.4;47.45763276703656;11.793185044080019;0;1;;Florian Seilmeier;K2-Ski;Rofanspitze ;Österreich
2025-01-12;Buchsteinhütte;Hike;1736700303219;12.19;11010;440;1281;38.9;47.639680076390505;11.67972944676876;0;0;Rodeln;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2025-01-14;Indoor Cycling;IndoorTrainer;1736862019205;32.09;3500;0;0;67.22;;;0;0;;;;;
2025-01-17;Indoor Cycling;IndoorTrainer;1737158348876;20.01;2946;0;0;49.75;;;0;0;;;;;
2025-01-19;Tschachaun;Skitour;1737299149318;18.92;18580;1395;2334;32.4;47.30819885618985;10.672822231426835;0;1;;Florian Seilmeier;K2-ATK;;Österreich
2025-01-22;Indoor Cycling;IndoorTrainer;1737567122026;13.35;2423;0;0;47.73;;;0;0;;;;;
2025-01-24;Naviser Kreuzjöchl ;Skitour;1737743346921;14.21;13812;1275;2529;47.6;47.13212130591273;11.598050398752093;0;1;;Jens Repp,Florian Seilmeier,Christian Kellner;K2-ATK;;Österreich
2025-01-25;Scheibenspitze;Skitour;1737823613812;20.09;21603;1835;2489;38.8;47.10539468564093;11.571688810363412;0;1;;Christian Kellner,Florian Seilmeier,Jens Repp;K2-ATK;Hohe Warte;Österreich
2025-01-26;Sattelberg;Skitour;1737907025374;11.38;11687;1365;2115;40.2;47.01100364327431;11.478831237182021;0;1;;Christian Kellner,Florian Seilmeier,Jens Repp;K2-ATK;;Österreich,Italien
2025-01-28;Indoor Cycling;IndoorTrainer;1738084686540;26.55;3090;0;0;48.1;;;0;0;;;;;
2025-02-01;Indoor Cycling;IndoorTrainer;1738436076219;40.08;4180;0;0;45.99;;;0;0;;;;;
2025-02-04;Indoor Cycling;IndoorTrainer;1738682620734;29.01;3608;0;0;49.31;;;0;0;;;;;
2025-02-06;Indoor Cycling;IndoorTrainer;1738864656894;34.38;3603;0;0;54.75;;;0;0;;;;;
2025-02-08;Jakobsbaiern;Mountainbike;1739023396867;31.59;4806;475;640;54.4;47.94459158554673;11.837173020467162;0;0;;;Canyon-MTB;;Deutschland
2025-02-11;Indoor Cycling;IndoorTrainer;1739290401446;30.25;4463;0;0;55.26;;;0;0;;;;;
2025-02-14;Indoor Cycling;IndoorTrainer;1739530093012;41.6;4415;0;0;53.01;;;0;0;;;;;
2025-02-19;Indoor Cycling;IndoorTrainer;1739989520200;16.95;1847;0;0;47.97;;;0;0;;;;;
2025-02-21;Schöntalspitze;Skitour;1740328136228;10.64;16583;1455;3002;39.4;47.12219160050154;11.094464231282473;0;1;;Florian Seilmeier;K2-ATK;Pforzheimer Hütte;Österreich
2025-02-22;Gleirscher Fernerkogel ;Skitour;1740328136226;14.87;19688;1235;3167;41.5;47.114131981506944;11.063552191480994;0;1;;Florian Seilmeier;K2-ATK;Pforzheimer Hütte,ac_id:1740328136228;Österreich
2025-02-23;Winnebacher Weißkogel;Skitour;1740328136223;20.04;23833;1240;3182;41.9;47.11067981086671;11.062320554628968;0;1;;Florian Seilmeier;K2-ATK;Pforzheimer Hütte,ac_id:1740328136226;Österreich
2025-02-25;Basis;IndoorTrainer;1740502636916;26.12;3097;0;0;43.64;;;0;0;;;;;
2025-02-27;Indoor Cycling;IndoorTrainer;1740685851566;24.96;3993;0;0;41.75;;;0;0;;;;;
2025-03-03;Hahnenkamm;Mountainbike;1740999661316;44.98;9508;1150;442;53.6;50.07821308448911;9.109585722908378;0;0;;;Peter's MTB bike;;Deutschland
2025-03-05;Indoor Cycling;IndoorTrainer;1741197900514;10.02;1859;0;0;37.45;;;0;0;;;;;
2025-03-04;Indoor Cycling;IndoorTrainer;1741197958241;14.22;2027;0;0;60.26;;;0;0;;;;;
2025-03-06;Piusheim;Racer;1741269048560;29.42;3673;410;642;62.9;47.9445915017277;11.837179055437446;0;0;;;Rose;;Deutschland
2025-01-31;Indoor Cycling;IndoorTrainer;1741290914488;20.12;2666;0;0;37.86;;;0;0;;;;;
2025-02-10;Indoor Cycling;IndoorTrainer;1741290914476;25.46;3612;0;0;47.73;;;0;0;;;;;
2025-02-13;Indoor Cycling;IndoorTrainer;1741290914472;21.51;2440;0;0;50.08;;;0;0;;;;;
2025-03-20;Haar;Racer;1742476163497;27.6;4216;90;596;38.4;48.00843000411987;11.79173145443201;0;0;;;Rose;;Deutschland
2025-03-22;Biberg;Racer;1742638890464;44.69;6392;670;641;60.9;47.94461530633271;11.83711476624012;0;0;;;Rose;;Deutschland
2025-03-24;Piusheim;Racer;1742839546122;29.38;3890;410;640;63.8;47.94459795579314;11.837160279974341;0;0;;;Rose;;Deutschland
2025-03-28;Gindelalm;Racer;1743168393203;80.39;13253;1050;1250;59.2;47.72542920894921;11.801718659698963;0;0;;Hanno Kaupp;Rose;;Deutschland
2025-03-28;Gindelalmschneid;Hike;1743169897973;0.67;751;80;1330;16.2;47.723476979881525;11.799031337723136;0;1;;;;;Deutschland
2025-03-30;Aschbach;Racer;1743339911622;33.09;4504;465;662;55.8;47.93979889713228;11.80640741251409;0;0;;;Rose;;Deutschland
2025-04-02;Feldkirchen-Westerham;Racer;1743594476227;34.51;4626;475;656;59.3;47.937939120456576;11.807430759072304;0;0;;;Rose;;Deutschland
2025-04-04;Schwärzscharte;Skitour;1743776236204;23.75;22499;1625;2404;49.4;47.351786931976676;10.915155708789825;0;0;;Florian Seilmeier;K2-ATK;;Österreich
2025-04-06;Dietersheim;Racer;1743941561084;41.36;5568;200;594;39.6;48.00894238986075;11.791553255170584;0;0;;;Rose;;Deutschland
2025-04-08;Aschbach;Racer;1744113890908;33.27;4268;465;658;59.7;47.93967945501208;11.806438760831952;0;0;;;Rose;;Deutschland
2025-04-10;Aschbach;Racer;1744284222695;33.27;4188;465;657;59.4;47.939700493589044;11.806438928470016;0;0;;;Rose;;Deutschland
2025-04-12;Aschbach;Racer;1744480625951;33.3;4622;465;655;59.4;47.939720526337624;11.806445717811584;0;0;;Peter Müller;Rose;;Deutschland
2025-04-15;Otterfing;Racer;1744717498304;33.35;3639;135;672;45.1;47.91508167050779;11.684094574302435;0;0;;;Rose;;Deutschland
2025-04-18;Otterfing-Biberg;Racer;1744974387801;69.28;9630;735;675;53.6;47.90939806960523;11.683270297944546;0;0;;;Rose;;Deutschland
2025-04-21;Aschbach-Jakobsbaiern;Racer;1745251867074;40.76;5284;600;655;57.6;47.9396113101393;11.80644161067903;0;0;;;Rose;;Deutschland
2025-04-23;Aschbach;Racer;1745410420301;33.12;4381;465;662;57.1;47.930307649075985;11.8117118999362;0;0;;;Rose;;Deutschland
2025-04-25;Unterföhring;Racer;1745597206438;53.22;7476;200;597;39.8;48.00892646424472;11.791788367554545;0;0;;;Rose;;Deutschland
2025-04-30;Karwendeltal;Mountainbike;1746025170307;21.36;5004;380;1300;36.7;47.43292652070522;11.366164293140173;0;0;;Florian Seilmeier;Canyon-MTB;ac_id:1746025190888;Österreich
2025-04-30;Große Seekarspitze;Skitour;1746025190888;12.14;16659;1385;2662;32.4;47.40491244010627;11.390701811760664;0;1;;Florian Seilmeier;K2-ATK;;Österreich
2025-05-02;Schliersbergalm;Hike;1746194814303;4.58;6212;270;1062;15.5;47.74044815450907;11.874951431527734;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2025-05-03;Josefsthaler Wasserfälle;Hike;1746297982854;6.88;8304;160;915;11.4;47.68454220145941;11.884171357378364;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz,Sonja Westermeyer,Hanno Kaupp,Fabian Knopf,Markus Thomann;;;
2025-05-07;Aschbach;Racer;1746615937985;33.12;4384;465;657;55.2;47.93963628821075;11.80645669810474;0;0;;;Rose;;Deutschland
2025-05-08;Brunnthal;Racer;1746730573556;24.01;3078;110;599;38.8;48.01484995521605;11.744235400110483;0;0;;;Rose;;Deutschland
2025-05-10;Rohrdorf;Racer;1746879233600;106.74;14532;875;660;52.0;47.93957191519439;11.80651344358921;0;0;;;Rose;;Deutschland
2025-05-13;Aschbach;Racer;1747142914096;33.14;4086;465;657;59.2;47.93045592494309;11.81165817193687;0;0;;;Rose;;Deutschland
2025-05-16;Sonnleiterhütte;Hike;1747407141906;3.09;5214;275;1513;8.13;47.41512645967305;12.286013374105096;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz;;;Österreich
2025-05-17;Gaisberg;Hike;1747493854178;5.35;9641;385;1770;9.0;47.42619048804045;12.287060776725411;0;1;;Joshua Prinz,Jonah Prinz,Mirjam Prinz,Chrissi Wolter,Patrick Wolter;;Sonnleiterhütte;Österreich
2025-05-18;Gampenkogel;Climb;1747592892832;6.94;13246;540;1957;6.38;47.404946722090244;12.265743082389235;0;1;;Jonah Prinz,Joshua Prinz,Mirjam Prinz;;Sonnleiterhütte;Österreich
2025-05-19;Brechhorn;Climb;1747634322577;11.86;8434;790;2032;15.2;47.37798255868256;12.266284469515085;0;0;;;;Sonnleiterhütte;Österreich
2025-05-19;Kreuzjoch;Hike;1747742984068;6.54;9943;290;1736;7.76;47.39752278663218;12.270496962592006;0;0;;Mirjam Prinz,Joshua Prinz,Jonah Prinz;;Sonnleiterhütte;Österreich
2025-05-20;Gaisbergjoch;Hike;1747742984065;7.31;8908;510;1753;13.3;47.41908112540841;12.283302498981357;0;0;;;;Sonnleiterhütte;Österreich
2025-05-21;Aschbach-Jakobsbaiern;Racer;1747841474664;40.6;5198;600;654;56.4;47.93963343836367;11.806452423334122;0;0;;;Rose;;Deutschland
2025-05-23;Taubenberg;Racer;1748009409031;56.39;7484;570;899;62.3;47.827460607513785;11.754385968670249;0;0;;;Rose;;Deutschland
2025-05-29;Chiemsee;Bicycle;1748546834506;46.8;16389;325;639;27.1;47.919807471334934;11.756097134202719;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;Rose,Thule;;Deutschland
2025-05-31;Chiemsee;Bicycle;1748889632264;48.23;13058;250;637;34.5;47.9235964268446;11.75630290992558;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;Rose;;Deutschland
2025-06-03;Glonn;Racer;1748965449612;24.61;3488;265;624;53.0;47.99895007163286;11.797624686732888;0;0;;;Rose;;Deutschland
2025-06-08;Grafing;Racer;1749395836786;39.08;5312;535;626;59.8;47.98810891807079;11.812823424115777;0;0;;Hanno Kaupp;Rose;;Deutschland
2025-06-09;Taubenberg;Hike;1749490307997;8.91;12792;280;909;8.67;47.82739497721195;11.7541138920933;0;0;;Joshua Prinz,Jonah Prinz,Mirjam Prinz;;;Deutschland
2025-06-10;Glonn;Racer;1749552425273;26.03;3522;325;635;57.2;47.95983424410224;11.834727767854929;0;0;;;Rose;;Deutschland
2025-06-12;Glonn;Bicycle;1749822043394;18.82;2566;200;629;54.4;47.98723887652159;11.81388876400888;0;0;;;Rose;;Deutschland
2025-06-13;Bucher Alm;Mountainbike;1749822087733;89.25;16642;1440;1241;52.3;47.72492512129247;11.978076584637165;0;0;;;Cube;;Deutschland
2025-06-13;Breitenstein;Climb;1749822192302;3.38;4553;385;1622;9.07;47.72111135534942;11.987767573446035;0;1;;;;ac_id:1749822087733;Deutschland
2025-06-17;Aschbach;Racer;1750155620043;33.1;4202;465;655;59.3;47.93968917801976;11.806420236825943;0;0;;;Rose;;Deutschland
2025-06-18;Aschbach;Racer;1750272064967;33.1;4071;465;654;59.5;47.93965816497803;11.8064328096807;0;0;;;Rose;;Deutschland
2025-06-22;Ödenpullach;Racer;1750615485535;39.92;5410;175;622;41.0;47.9939832072705;11.564952861517668;0;0;;Christian Kellner;Rose;;Deutschland
2025-06-24;Kleinhöhenkirchen;Racer;1750764773924;32.3;4718;500;657;54.6;47.91237766854465;11.790828388184309;0;0;;;Rose;;Deutschland
2025-06-24;Kastensee;Bicycle;1750791520497;8.92;4325;145;635;29.6;47.9976028483361;11.819549482315779;0;0;;Joshua Prinz,Jonah Prinz;Scott;;Deutschland
2025-06-25;Holzkirchen;Racer;1750849426815;44.16;5880;230;695;41.2;47.875626711174846;11.6964624915272;0;0;;;Rose;;Deutschland
2025-06-27;Gmund;Racer;1751019410826;75.0;10269;820;862;51.6;47.75561395101249;11.784714041277766;0;0;;;Rose;;Deutschland
2025-06-29;Ellmauer Halt;Climb;1751196709385;11.46;15847;1295;2336;9.1;47.56164279766381;12.302529914304614;0;1;;Hanno Kaupp;;;Österreich
2025-06-29;Sauerlach;Racer;1751368963038;20.68;2902;90;622;36.8;47.97711689025164;11.68344740755856;0;0;;;Rose;;Deutschland
2025-07-01;Kleinhöhenkirchen;Racer;1751368980391;32.24;4379;500;657;57.2;47.91235118173063;11.790891336277127;0;0;;;Rose;;Deutschland
2025-07-01;Steinsee;Bicycle;1751395526308;16.91;4748;190;634;37.5;48.002483546733856;11.80283010005951;0;0;;Joshua Prinz,Jonah Prinz;Scott;;Deutschland
2025-07-03;Kleinhöhenkirchen;Racer;1751541007923;32.22;4281;500;657;55.6;47.91231899522245;11.790960486978292;0;0;;;Rose;;Deutschland
2025-07-05;Kleinhöhenkirchen - Jakobsbaiern;Racer;1751709060899;55.28;7810;870;659;53.5;47.912316396832466;11.790943052619696;0;0;;;Rose;;Deutschland
        """.trimIndent()
    }
}