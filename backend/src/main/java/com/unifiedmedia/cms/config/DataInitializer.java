package com.unifiedmedia.cms.config;

import com.unifiedmedia.cms.entity.Language;
import com.unifiedmedia.cms.entity.StorageNode;
import com.unifiedmedia.cms.entity.Template;
import com.unifiedmedia.cms.repository.LanguageRepository;
import com.unifiedmedia.cms.repository.StorageNodeRepository;
import com.unifiedmedia.cms.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StorageNodeRepository storageNodeRepository;
    private final TemplateRepository templateRepository;
    private final LanguageRepository languageRepository;

    @Override
    public void run(String... args) {
        initStorageNode();
        initDefaultTemplate();
        initLanguages();
    }

    private void initStorageNode() {
        if (storageNodeRepository.count() > 0) return;
        StorageNode node = StorageNode.builder()
                .name("本地存储")
                .providerType("LOCAL")
                .connectionConfig("{\"basePath\": \"/\"}")
                .isReadonly(false)
                .build();
        storageNodeRepository.save(node);
        log.info("[Init] Created default storage node: 本地存储 id={}", node.getId());
    }

    private void initDefaultTemplate() {
        if (templateRepository.count() > 0) return;
        String graph = """
            {
              "nodes": [
                {"id":"FileSnifferNode_0","name":FileSnifferNode.NODE_NAME,"label":"文件嗅探","icon":"🔍","config":{},"condition":""},
                {"id":"RouterNode_1","name":"RouterNode","label":"条件分支","icon":"🔀","config":{},"condition":"detectedFormat == 'TXT'"},
                {"id":"TxtExtractorNode_2","name":"TxtExtractorNode","label":"TXT 采样","icon":"📄","config":{},"condition":""},
                {"id":"EpubMetaParserNode_3","name":"EpubMetaParserNode","label":"EPUB 解析","icon":"📖","config":{},"condition":""},
                {"id":"LlmAnalyzerNode_4","name":"LlmAnalyzerNode","label":"大模型分析","icon":"🤖","config":{},"condition":""},
                {"id":"DoubanScraperNode_5","name":"DoubanScraperNode","label":"豆瓣刮削","icon":"🌐","config":{},"condition":""},
                {"id":"FormatConverterNode_6","name":"FormatConverterNode","label":"TXT→EPUB","icon":"🔄","config":{},"condition":""},
                {"id":"ArchiveNode_7","name":"ArchiveNode","label":"归档写入","icon":"📦","config":{},"condition":""}
              ],
              "edges": [
                {"id":"e-0","source":"FileSnifferNode_0","target":"RouterNode_1","sourcePort":"default"},
                {"id":"e-1","source":"RouterNode_1","target":"TxtExtractorNode_2","sourcePort":"true"},
                {"id":"e-2","source":"RouterNode_1","target":"EpubMetaParserNode_3","sourcePort":"false"},
                {"id":"e-3","source":"TxtExtractorNode_2","target":"LlmAnalyzerNode_4","sourcePort":"default"},
                {"id":"e-4","source":"EpubMetaParserNode_3","target":"LlmAnalyzerNode_4","sourcePort":"default"},
                {"id":"e-5","source":"LlmAnalyzerNode_4","target":"DoubanScraperNode_5","sourcePort":"default"},
                {"id":"e-6","source":"DoubanScraperNode_5","target":"FormatConverterNode_6","sourcePort":"default"},
                {"id":"e-7","source":"FormatConverterNode_6","target":"ArchiveNode_7","sourcePort":"default"}
              ]
            }
            """;
        Template template = Template.builder()
                .name("default-book-pipeline")
                .description("默认图书处理流水线")
                .graphPayload(graph)
                .isDefault(true)
                .build();
        templateRepository.save(template);
        log.info("[Init] Created default template: default-book-pipeline");
    }

    private void initLanguages() {
        if (languageRepository.count() > 0) return;
        List<Language> languages = List.of(
            lang("zh-CN","汉语（简体）","Chinese","中文"),
            lang("zh-TW","汉语（繁体）","Chinese","中繁"),
            lang("en","英语","English","英文"),
            lang("es","西班牙语","Español","西语"),
            lang("fr","法语","Français","法语"),
            lang("ar","阿拉伯语","العربية","阿语"),
            lang("ru","俄语","Русский","俄语"),
            lang("de","德语","Deutsch","德语"),
            lang("ja","日语","日本語","日语"),
            lang("pt","葡萄牙语","Português","葡语"),
            lang("it","意大利语","Italiano","意语"),
            lang("ko","韩语","한국어","韩语"),
            lang("tr","土耳其语","Türkçe","土语"),
            lang("vi","越南语","Tiếng Việt","越语"),
            lang("hi","印地语","हिन्दी","印地"),
            lang("bn","孟加拉语","বাংলা","孟语"),
            lang("nl","荷兰语","Nederlands","荷语"),
            lang("sv","瑞典语","Svenska","瑞典"),
            lang("no","挪威语","Norsk","挪威"),
            lang("da","丹麦语","Dansk","丹麦"),
            lang("fi","芬兰语","Suomi","芬兰"),
            lang("is","冰岛语","Íslenska","冰岛"),
            lang("el","希腊语","Ελληνικά","希语"),
            lang("pl","波兰语","Polski","波兰"),
            lang("cs","捷克语","Čeština","捷克"),
            lang("hu","匈牙利语","Magyar","匈语"),
            lang("ro","罗马尼亚语","Română","罗语"),
            lang("bg","保加利亚语","Български","保语"),
            lang("sr","塞尔维亚语","Српски","塞尔维"),
            lang("hr","克罗地亚语","Hrvatski","克罗地亚"),
            lang("sl","斯洛文尼亚语","Slovenščina","斯洛文尼亚"),
            lang("lt","立陶宛语","Lietuvių","立陶宛"),
            lang("lv","拉脱维亚语","Latviešu","拉脱维亚"),
            lang("et","爱沙尼亚语","Eesti","爱沙尼亚"),
            lang("eu","巴斯克语","Euskara","巴斯克"),
            lang("cy","威尔士语","Cymraeg","威尔士"),
            lang("gd","苏格兰盖尔语","Gàidhlig","盖尔语"),
            lang("th","泰语","ภาษาไทย","泰语"),
            lang("id","印尼语","Bahasa Indonesia","印尼"),
            lang("ms","马来语","Bahasa Melayu","马来"),
            lang("fa","波斯语","فارسی","波斯"),
            lang("ur","乌尔都语","اردو","乌尔都"),
            lang("sw","斯瓦希里语","Kiswahili","斯瓦希里"),
            lang("ha","豪萨语","Hausa","豪萨"),
            lang("he","希伯来语","עברית","希伯来"),
            lang("ka","格鲁吉亚语","ქართული","格鲁吉亚"),
            lang("hy","亚美尼亚语","Հայերեն","亚美尼亚"),
            lang("la","拉丁语","Latina","拉丁"),
            lang("sa","梵语","संस्कृतम्","梵语"),
            lang("grc","古希腊语","Ἑλληνικά","古希腊"),
            lang("mn","蒙古语（蒙文）","Монгол","蒙语"),
            lang("bo","藏语","བོད་སྐད་","藏语"),
            lang("ug","维吾尔语","ئۇيغۇرچە","维语"),
            lang("kk","哈萨克语","Қазақша","哈语"),
            lang("za","壮语","Vahcuengh","壮语"),
            lang("ii","彝语","ꆈꌠꉙ","彝语"),
            lang("hmn","苗语","Hmong","苗语"),
            lang("tai","傣语","Tai","傣语"),
            lang("lis","傈僳语","Lisu","傈僳语"),
            lang("lhu","拉祜语","Lahu","拉祜语"),
            lang("nax","纳西语","Naxi","纳西语"),
            lang("jph","景颇语","Jingpho","景颇语"),
            lang("ky","柯尔克孜语","Кыргызча","柯尔克孜"),
            lang("sjo","锡伯语","Xibe","锡伯语"),
            lang("tg","塔吉克语","Тоҷикӣ","塔吉克"),
            lang("uz","乌孜别克语","Oʻzbekcha","乌兹别克"),
            lang("tt","塔塔尔语","Татарча","塔塔尔")
            // ko (朝鲜语) & ru (俄语境内) share codes with standard entries, skipped
        );
        languageRepository.saveAll(languages);
        log.info("[Init] Seeded {} languages", languages.size());
    }

    private Language lang(String code, String name, String nativeName, String shortName) {
        return Language.builder().code(code).name(name).nativeName(nativeName).shortName(shortName).build();
    }
}
