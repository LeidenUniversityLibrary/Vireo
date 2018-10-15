package org.tdl.vireo.utility;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tdl.vireo.model.Submission;
import org.tdl.vireo.model.formatter.Formatter;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

@Service
public class FormatterUtility {

    @Autowired
    private SpringTemplateEngine templateEngine;

//    public Optional<String> renderManifest(Formatter formatter, Submission submission) throws Exception {
//        Context context = new Context(Locale.getDefault());
//        formatter.populateContext(context, submission);
//        Optional<String> template = Optional.of(formatter.getTemplate());
//        Optional<String> manifest = Optional.empty();
//        if (template.isPresent()) {
//            manifest = Optional.of(templateEngine.process(template.get(), context));
//        }
//        return manifest;
//    }

    public Optional<Map<String, String>> renderManifestMap(Formatter formatter, Submission submission) throws Exception {
        Map<String, String> renderMap = new HashMap<String, String>();
        Context context = new Context(Locale.getDefault());
        formatter.populateContext(context, submission);
        Optional<Map<String, String>> templates = Optional.of(formatter.getTemplates());
        Optional<String> dcContent = Optional.empty();
        if (templates.isPresent() && templates.get().size() > 0) {
            for (Map.Entry<String, String> template : templates.get().entrySet()) {
                dcContent = Optional.of(templateEngine.process(template.getValue(), context));
                if (dcContent.isPresent()) {
                    renderMap.put(template.getKey(), dcContent.get());
                }
            }
        }
        return Optional.of(renderMap);
    }

}
