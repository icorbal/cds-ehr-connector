package com.marand.thinkehr.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.openehr.jaxb.rm.DvUri;
import org.openehr.jaxb.rm.Element;
import org.openehr.jaxb.rm.TerminologyId;
import org.openehr.rm.datatypes.basic.DataValue;
import org.openehr.rm.datatypes.basic.DvBoolean;
import org.openehr.rm.datatypes.quantity.DvAmount;
import org.openehr.rm.datatypes.quantity.DvCount;
import org.openehr.rm.datatypes.quantity.DvInterval;
import org.openehr.rm.datatypes.quantity.DvOrdered;
import org.openehr.rm.datatypes.quantity.DvOrdinal;
import org.openehr.rm.datatypes.quantity.DvProportion;
import org.openehr.rm.datatypes.quantity.DvQuantity;
import org.openehr.rm.datatypes.quantity.ProportionKind;
import org.openehr.rm.datatypes.quantity.ReferenceRange;
import org.openehr.rm.datatypes.quantity.datetime.DvDate;
import org.openehr.rm.datatypes.quantity.datetime.DvDateTime;
import org.openehr.rm.datatypes.quantity.datetime.DvDuration;
import org.openehr.rm.datatypes.quantity.datetime.DvTemporal;
import org.openehr.rm.datatypes.quantity.datetime.DvTime;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.datatypes.text.DvCodedText;
import org.openehr.rm.datatypes.text.DvText;
import org.openehr.rm.datatypes.uri.DvURI;
import org.openehr.rm.support.identification.TerminologyID;

/**
 * @author Jure Grom
 */
public class RMConvertUtil
{
  private RMConvertUtil()
  {
  }

  public static <T extends DataValue, F extends org.openehr.jaxb.rm.DataValue> List<T> convert(List<F> values, Class<T> type)
  {
    if (values == null) return null;
    final List<T> convertedValues = new ArrayList<T>();
    for (F value : values)
    {
      convertedValues.add((T)convert(value));
    }
    return convertedValues;
  }

  public static <T extends DvOrdered> ReferenceRange<T> convert(org.openehr.jaxb.rm.ReferenceRange value)
  {
    return new ReferenceRange<T>(
        convert(value.getMeaning()),
        (DvInterval<T>)convert(value.getRange())
    );
  }

  public static <T extends DvOrdered> DvInterval<T> convert(org.openehr.jaxb.rm.DvInterval value)
  {
    return new DvInterval<T>(
        (T)convert(value.getLower()),
        (T)convert(value.getUpper())
    );
  }

  public static DataValue convert(Object o)
  {
    if (o instanceof Element)
    {
      return convert(((Element)o).getValue());
    }
    else if (o instanceof org.openehr.jaxb.rm.DataValue)
    {
      return convert((org.openehr.jaxb.rm.DataValue)o);
    }
    else
    {
      throw new UnsupportedOperationException("Unexpected type "+o.getClass().getSimpleName());
    }
  }

  public static DataValue convert(org.openehr.jaxb.rm.DataValue value)
  {
    if (value instanceof org.openehr.jaxb.rm.DvOrdinal)
    {
      return convert((org.openehr.jaxb.rm.DvOrdinal)value);
    }
    else if (value instanceof org.openehr.jaxb.rm.DvAmount)
    {
      if (value instanceof org.openehr.jaxb.rm.DvDuration)
      {
        return convert((org.openehr.jaxb.rm.DvDuration)value);
      }
      else if (value instanceof org.openehr.jaxb.rm.DvCount)
      {
        return convert((org.openehr.jaxb.rm.DvCount)value);
      }
      else if (value instanceof org.openehr.jaxb.rm.DvProportion)
      {
        return convert((org.openehr.jaxb.rm.DvProportion)value);
      }
      else if (value instanceof org.openehr.jaxb.rm.DvQuantity)
      {
        return convert((org.openehr.jaxb.rm.DvQuantity)value);
      }
      /*else
      {
        return convert((org.openehr.jaxb.rm.DvAmount)value);
      } */
    }
    else if (value instanceof org.openehr.jaxb.rm.DvTemporal)
    {
      if (value instanceof org.openehr.jaxb.rm.DvTime)
      {
        return convert((org.openehr.jaxb.rm.DvTime)value);
      }
      else if (value instanceof org.openehr.jaxb.rm.DvDateTime)
      {
        return convert((org.openehr.jaxb.rm.DvDateTime)value);
      }
      else if (value instanceof org.openehr.jaxb.rm.DvDate)
      {
        return convert((org.openehr.jaxb.rm.DvDate)value);
      }
      /*else
      {
        return convert((org.openehr.jaxb.rm.DvTemporal)value);
      } */
    }
    else if (value instanceof org.openehr.jaxb.rm.DvText)
    {
      if (value instanceof org.openehr.jaxb.rm.DvCodedText)
      {
        return convert((org.openehr.jaxb.rm.DvCodedText)value);
      }
      else
      {
        return convert((org.openehr.jaxb.rm.DvText)value);
      }
    }
    else if (value instanceof org.openehr.jaxb.rm.DvInterval)
    {
      return convert((org.openehr.jaxb.rm.DvInterval)value);
    }
    else if (value instanceof org.openehr.jaxb.rm.DvBoolean)
    {
      return convert((org.openehr.jaxb.rm.DvBoolean)value);
    }

    return DataValue.parseValue(value.toString());
  }

  public static DvCount convert(org.openehr.jaxb.rm.DvCount value)
  {
    return new DvCount((int)value.getMagnitude());
  }

  public static DvOrdinal convert(org.openehr.jaxb.rm.DvOrdinal value)
  {
    return new DvOrdinal(value.getValue(),convert(value.getSymbol()));
  }

  public static DataValue convert(org.openehr.jaxb.rm.DvQuantity value)
  {
    return new DvQuantity(
        value.getUnits(),
        value.getMagnitude(),
        value.getPrecision()
    );
  }

  public static DvDate convert(org.openehr.jaxb.rm.DvDate value)
  {
    return new DvDate(
        value.getValue()
    );
  }

  public static DvDateTime convert(org.openehr.jaxb.rm.DvDateTime value)
  {
    return new DvDateTime(value.getValue());
  }

  public static DvDuration convert(org.openehr.jaxb.rm.DvDuration value)
  {
    return new DvDuration(value.getValue());
  }

  public static DvTime convert(org.openehr.jaxb.rm.DvTime value)
  {
    return new DvTime(value.getValue());
  }

  public static DvProportion convert(org.openehr.jaxb.rm.DvProportion value)
  {
    return new DvProportion(
        value.getNumerator(), value.getDenominator(), value.getType()!=null ? ProportionKind.fromValue(
        value.getType()
            .intValue()) : ProportionKind.FRACTION, value.getPrecision()
    );
  }

  public static DvBoolean convert(org.openehr.jaxb.rm.DvBoolean value)
  {
    return new DvBoolean(value.isValue());
  }

  public static DvCodedText convert(org.openehr.jaxb.rm.DvCodedText value)
  {
    return new DvCodedText(value.getValue(), convert(value.getDefiningCode()));
  }

  public static CodePhrase convert(org.openehr.jaxb.rm.CodePhrase value)
  {
    return new CodePhrase(value.getTerminologyId().getValue(),value.getCodeString());
  }

  public static DvText convert(org.openehr.jaxb.rm.DvText value)
  {
    return new DvText(value.getValue());
  }

  public static org.openehr.jaxb.rm.DataValue convert(DataValue value)
  {
    if (value == null)
    {
      return null;
    }

    if (value instanceof DvOrdinal)
    {
      return convert((DvOrdinal)value);
    }
    else if (value instanceof DvAmount)
    {
      if (value instanceof DvDuration)
      {
        return convert((DvDuration)value);
      }
      else if (value instanceof DvCount)
      {
        return convert((DvCount)value);
      }
      else if (value instanceof DvProportion)
      {
        return convert((DvProportion)value);
      }
      else if (value instanceof DvQuantity)
      {
        return convert((DvQuantity)value);
      }
    }
    else if (value instanceof DvTemporal)
    {
      if (value instanceof DvTime)
      {
        return convert((DvTime)value);
      }
      else if (value instanceof DvDateTime)
      {
        return convert((DvDateTime)value);
      }
      else if (value instanceof DvDate)
      {
        return convert((DvDate)value);
      }
      /*else
      {
        return convert((org.openehr.jaxb.rm.DvTemporal)value);
      } */
    }
    else if (value instanceof DvText)
    {
      if (value instanceof DvCodedText)
      {
        return convert((DvCodedText)value);
      }
      else
      {
        return convert((DvText)value);
      }
    }
    else if (value instanceof DvInterval)
    {
      return convert((DvInterval)value);
    }
    else if (value instanceof DvBoolean)
    {
      return convert((DvBoolean)value);
    }

    throw new UnsupportedOperationException("Unhandled type "+value.getClass().getSimpleName());
  }

  public static org.openehr.jaxb.rm.DvOrdinal convert(DvOrdinal value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvOrdinal v = new org.openehr.jaxb.rm.DvOrdinal();
    v.setSymbol(convert(value.getSymbol()));
    v.setValue(value.getValue());
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    return v;
  }

  public static org.openehr.jaxb.rm.DvDuration convert(DvDuration value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvDuration v = new org.openehr.jaxb.rm.DvDuration();
    v.setValue(value.getValue());
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy((float)value.getAccuracy());
    v.setAccuracyIsPercent(value.isAccuracyPercent());
    return v;
  }

  public static org.openehr.jaxb.rm.CodePhrase convert(CodePhrase value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.CodePhrase v = new org.openehr.jaxb.rm.CodePhrase();
    v.setCodeString(value.getCodeString());
    v.setTerminologyId(convert(value.getTerminologyId()));
    return v;
  }

  public static TerminologyId convert(TerminologyID value)
  {
    if (value == null) return null;
    TerminologyId v = new TerminologyId();
    v.setValue(value.getValue());
    return v;
  }

  public static org.openehr.jaxb.rm.DvCount convert(DvCount value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvCount v = new org.openehr.jaxb.rm.DvCount();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy((float)value.getAccuracy());
    v.setAccuracyIsPercent(value.isAccuracyPercent());
    v.setMagnitude(value.getMagnitude());
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    return v;
  }

  public static org.openehr.jaxb.rm.DvProportion convert(DvProportion value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvProportion v = new org.openehr.jaxb.rm.DvProportion();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy((float)value.getAccuracy());
    v.setAccuracyIsPercent(value.isAccuracyPercent());
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    v.setDenominator((float)value.getDenominator());
    v.setNumerator((float)value.getNumerator());
    v.setPrecision(value.getPrecision());
    if (value.getType() != null)
    {
      v.setType(BigInteger.valueOf(value.getType().getValue()));
    }
    return v;
  }

  public static org.openehr.jaxb.rm.DvQuantity convert(DvQuantity value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvQuantity v = new org.openehr.jaxb.rm.DvQuantity();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy((float)value.getAccuracy());
    v.setAccuracyIsPercent(value.isAccuracyPercent());
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    v.setMagnitude(value.getMagnitude());
    v.setPrecision(value.getPrecision());
    v.setUnits(value.getUnits());
    return v;
  }

  public static org.openehr.jaxb.rm.DvTime convert(DvTime value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvTime v = new org.openehr.jaxb.rm.DvTime();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy(convert(value.getAccuracy()));
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    v.setValue(value.getValue());
    return v;
  }

  public static org.openehr.jaxb.rm.DvDateTime convert(DvDateTime value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvDateTime v = new org.openehr.jaxb.rm.DvDateTime();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy(convert(value.getAccuracy()));
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    v.setValue(value.getValue());
    return v;
  }

  public static org.openehr.jaxb.rm.DvDate convert(DvDate value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvDate v = new org.openehr.jaxb.rm.DvDate();
    v.setNormalStatus(convert(value.getNormalStatus()));
    v.setNormalRange(convert(value.getNormalRange()));
    v.setAccuracy(convert(value.getAccuracy()));
    v.setMagnitudeStatus(value.getMagnitudeStatus());
    v.setValue(value.getValue());

    return v;
  }

  public static org.openehr.jaxb.rm.DvCodedText convert(DvCodedText value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvCodedText v = new org.openehr.jaxb.rm.DvCodedText();
    v.setDefiningCode(convert(value.getDefiningCode()));
    v.setEncoding(convert(value.getEncoding()));
    v.setFormatting(value.getFormatting());
    v.setLanguage(convert(value.getLanguage()));
    v.setValue(value.getValue());
    v.setHyperlink(convert(value.getHyperlink()));
    return v;
  }

  public static DvUri convert(DvURI value)
  {
    if (value == null) return null;
    DvUri v = new DvUri();
    v.setValue(value.getValue());
    return v;
  }

  public static org.openehr.jaxb.rm.DvText convert(DvText value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvText v = new org.openehr.jaxb.rm.DvText();
    v.setEncoding(convert(value.getEncoding()));
    v.setFormatting(value.getFormatting());
    v.setLanguage(convert(value.getLanguage()));
    v.setValue(value.getValue());
    v.setHyperlink(convert(value.getHyperlink()));
    return v;
  }

  public static org.openehr.jaxb.rm.DvInterval convert(DvInterval value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvInterval v = new org.openehr.jaxb.rm.DvInterval();
    v.setLower((org.openehr.jaxb.rm.DvOrdered)convert(value.getLower()));
    v.setLowerIncluded(value.isLowerIncluded());
    v.setLowerUnbounded(value.isLowerUnbounded());
    v.setUpper((org.openehr.jaxb.rm.DvOrdered)convert(value.getUpper()));
    v.setUpperIncluded(value.isUpperIncluded());
    v.setUpperUnbounded(value.isUpperUnbounded());
    return v;
  }

  public static org.openehr.jaxb.rm.DvBoolean convert(DvBoolean value)
  {
    if (value == null) return null;
    org.openehr.jaxb.rm.DvBoolean v = new org.openehr.jaxb.rm.DvBoolean();
    v.setValue(value.getValue());
    return v;
  }
}
