package beta1.collafis.web;

import beta1.collafis.entry.MultiCriteriaEntry;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/multicriteriaentrys")
@Controller
@RooWebScaffold(path = "multicriteriaentrys", formBackingObject = MultiCriteriaEntry.class)
public class MultiCriteriaEntryController {
}
