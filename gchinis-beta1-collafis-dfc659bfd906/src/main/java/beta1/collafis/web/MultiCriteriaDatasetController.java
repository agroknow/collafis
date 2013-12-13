package beta1.collafis.web;

import beta1.collafis.dataset.Dataset;
import beta1.collafis.dataset.MultiCriteriaDataset;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/multicriteriadatasets")
@Controller
@RooWebScaffold(path = "multicriteriadatasets", formBackingObject = MultiCriteriaDataset.class)
public class MultiCriteriaDatasetController {

    private static final File CSV_DIR = new File("src/main/resources/csv");

    private static final String DATASET_FILENAME = "cfm_preferences_mend2_mini.csv";

    private static final File GROUPLENS_DIR = new File("src/main/resources/grouplens");

    private static final String GROUPLENS_FILENAME = "ratings.dat";

    private static final Logger log = LoggerFactory.getLogger(MultiCriteriaDatasetController.class);


    @RequestMapping("/mendeley_init")
    public String mendeley_init(Model uiModel, HttpServletRequest request) {
        Dataset dataset;
        try {
            dataset = MultiCriteriaDataset.MendeleyFromCsvFile("mendeley", new File(CSV_DIR, DATASET_FILENAME));
            uiModel.addAttribute("dataset", dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "multicriteriadatasets/init";
    }

    @RequestMapping("/lenskit_init")
    public String lenskit_init(Model uiModel, HttpServletRequest request) {
        Dataset dataset;
        try {
            dataset = MultiCriteriaDataset.LenskitFromCsvFile("lenskit", new File(GROUPLENS_DIR, GROUPLENS_FILENAME));
            uiModel.addAttribute("dataset", dataset);
            System.out.println("Lenskit size:" + dataset.getRatings().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "multicriteriadatasets/init";
    }
}
