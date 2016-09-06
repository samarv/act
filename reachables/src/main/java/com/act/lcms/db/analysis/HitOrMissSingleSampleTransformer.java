package com.act.lcms.db.analysis;

import com.act.lcms.db.io.report.IonAnalysisInterchangeModel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class HitOrMissSingleSampleTransformer extends HitOrMissTransformer<IonAnalysisInterchangeModel.HitOrMiss> {

  private Double minIntensityThreshold;
  private Double minSnrThreshold;
  private Double minTimeThreshold;
  private Set<String> ions;

  public HitOrMissSingleSampleTransformer(Double minIntensityThreshold, Double minSnrThreshold, Double minTimeThreshold,
                                          Set<String> ions) {
    this.minIntensityThreshold = minIntensityThreshold;
    this.minSnrThreshold = minSnrThreshold;
    this.minTimeThreshold = minTimeThreshold;
    this.ions = ions;
  }

  public Pair<IonAnalysisInterchangeModel.HitOrMiss, Boolean> apply(IonAnalysisInterchangeModel.HitOrMiss hitOrMissMolecule) {
    Double intensity = hitOrMissMolecule.getIntensity();
    Double snr = hitOrMissMolecule.getSnr();
    Double time = hitOrMissMolecule.getTime();
    String ion = hitOrMissMolecule.getIon();

    IonAnalysisInterchangeModel.HitOrMiss molecule = new IonAnalysisInterchangeModel.HitOrMiss(
        hitOrMissMolecule.getInchi(), ion, snr, time, intensity, hitOrMissMolecule.getPlot());

    // If the intensity, snr and time pass the thresholds set AND the ion of the peak molecule is within the set of
    // ions we want extracted, we keep the molecule. Else, we throw it away.
    if (intensity > minIntensityThreshold && snr > minSnrThreshold && time > minTimeThreshold &&
        (ions.size() == 0 || ions.contains(ion))) {
      return Pair.of(molecule, DO_NOT_THROW_OUT_MOLECULE);
    } else {
      return Pair.of(molecule, THROW_OUT_MOLECULE);
    }
  }
}
