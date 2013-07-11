import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import se.cambio.cds.controller.guide.GuideUtil;
import se.cambio.cds.model.facade.ehr.delegate.EHRFacadeDelegate;
import se.cambio.cds.model.facade.ehr.thinkehr.ThinkEHREHRFacadeDelegateImpl;
import se.cambio.cds.model.facade.execution.vo.GeneratedArchetypeReference;
import se.cambio.cds.model.facade.execution.vo.GeneratedElementInstance;
import se.cambio.cds.model.instance.ArchetypeReference;
import se.cambio.cds.model.instance.ElementInstance;
import se.cambio.cds.util.AggregationFunctions;
import se.cambio.cds.util.Domains;

/**
 * @author Jure Grom
 */
public class TestEhrFacadeDelegate
{
  public static void main(String[] args) throws Exception
  {
    //String thinkEhrHost = args[0];
    //String thinkEhrPort = args[1];
    //String thinkEhrUser = args[2];
    //String thinkEhrPass = args[3];
    //String thinkEhrNmsp = args[4];
    String extPatientId = "9044408";

    /*
    EHRFacadeDelegate ehrFacadeDelegate = new ThinkEHREHRFacadeDelegateImpl(
        ThinkEhrServiceFactory.getThinkEhrService(thinkEhrHost,Integer.parseInt(thinkEhrPort)),
        thinkEhrUser,
        thinkEhrPass,
        thinkEhrNmsp
    );*/

    EHRFacadeDelegate ehrFacadeDelegate = new ThinkEHREHRFacadeDelegateImpl();
    
    String ehrId = ehrFacadeDelegate.getEHRIds(Collections.singleton(extPatientId)).iterator().next();

    final Collection<ArchetypeReference> archetypeReferences = new ArrayList<ArchetypeReference>();
    final GeneratedArchetypeReference archetypeReference2 = new GeneratedArchetypeReference(
        Domains.EHR_ID,
        "openEHR-EHR-OBSERVATION.body_weight.v1",
        null,
        AggregationFunctions.ID_AGGREGATION_FUNCTION_LAST
    );
    archetypeReferences.add(archetypeReference2);
    archetypeReference2.getElementInstancesMap().put(
        "openEHR-EHR-OBSERVATION.body_weight.v1/data[at0002]/events[at0003]/data[at0001]/items[at0004]",
        new GeneratedElementInstance(
            "openEHR-EHR-OBSERVATION.body_weight.v1/data[at0002]/events[at0003]/data[at0001]/items[at0004]",
            null,
            archetypeReference2,
            null,
            GuideUtil.NULL_FLAVOUR_CODE_NO_INFO,
            "BMI.Calculation.v.1",
            "gt0002"
        )
    );
    final GeneratedArchetypeReference archetypeReference1 = new GeneratedArchetypeReference(
        Domains.EHR_ID,
        "openEHR-EHR-OBSERVATION.body_weight.v1",
        null,
        null
    );
    archetypeReferences.add(archetypeReference1);
    archetypeReference1.getElementInstancesMap().put(
        "openEHR-EHR-OBSERVATION.body_weight.v1/data[at0002]/events[at0003]/data[at0001]/items[at0004]",
        new GeneratedElementInstance(
            "openEHR-EHR-OBSERVATION.body_weight.v1/data[at0002]/events[at0003]/data[at0001]/items[at0004]",
            null,
            archetypeReference1,
            null,
            GuideUtil.NULL_FLAVOUR_CODE_NO_INFO,
            "BMI.Calculation.v.1",
            "gt0002"
        )
    );
    final GeneratedArchetypeReference archetypeReference = new GeneratedArchetypeReference(
        Domains.EHR_ID,
        "openEHR-EHR-OBSERVATION.height.v1",
        null,
        null//AggregationFunctions.ID_AGGREGATION_FUNCTION_LAST
    );
    archetypeReferences.add(archetypeReference);
    archetypeReference.getElementInstancesMap().put(
        "openEHR-EHR-OBSERVATION.height.v1/data[at0001]/events[at0002]/data[at0003]/items[at0004]",
        new GeneratedElementInstance(
            "openEHR-EHR-OBSERVATION.height.v1/data[at0001]/events[at0002]/data[at0003]/items[at0004]",
            null,
            archetypeReference,
            null,
            GuideUtil.NULL_FLAVOUR_CODE_NO_INFO,
            "BMI.Calculation.v.1",
            "gt0003"
        )
    );
    archetypeReference.getElementInstancesMap().put(
        "openEHR-EHR-OBSERVATION.height.v1/data[at0001]/events[at0002]/time",
        new GeneratedElementInstance(
            "openEHR-EHR-OBSERVATION.height.v1/data[at0001]/events[at0002]/time",
            null,
            archetypeReference,
            null,
            GuideUtil.NULL_FLAVOUR_CODE_NO_INFO,
            "BMI.Calculation.v.1",
            "gt0004"
        )
    );



    Collection<ElementInstance> result = ehrFacadeDelegate.queryEHRElements(ehrId, archetypeReferences);

    System.out.println("result.iterator().next() = " + result.iterator().next());
  }
}
