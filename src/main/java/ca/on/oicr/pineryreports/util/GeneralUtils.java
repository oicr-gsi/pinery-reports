package ca.on.oicr.pineryreports.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ca.on.oicr.ws.dto.InstrumentDto;
import ca.on.oicr.ws.dto.InstrumentModelDto;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.UserDto;

public class GeneralUtils {

  private GeneralUtils() {
    throw new IllegalStateException("Util class not intended for instantiation");
  }

  public static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";

  public static final String RUN_FAILED = "Failed";
  public static final String RUN_COMPLETED = "Completed";

  /**
   * Removes the time portion of a date/time String
   * 
   * @param datetime
   *          String in format "YYYY-MM-DD..."
   * @return String in format "YYYY-MM-DD" (anything beyond this is truncated)
   */
  public static String removeTime(String datetime) {
    if (datetime == null) {
      return null;
    }
    Matcher m = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}).*").matcher(datetime);
    if (!m.matches()) {
      throw new IllegalArgumentException("Datetime string is not in expected format (YYYY-MM-DD...)");
    }
    return m.group(1);
  }
  
  public static Predicate<RunDto> byEndedBetween(String start, String end) {
    return dto -> (start == null || 
        (dto.getCompletionDate() != null && dto.getCompletionDate().compareTo(start) > 0))
      && (end == null || 
        (dto.getCompletionDate() != null && dto.getCompletionDate().compareTo(end) < 0));
  }

  public static Map<Integer, UserDto> mapUsersById(Collection<UserDto> users) {
    return users.stream()
        .collect(Collectors.toMap(UserDto::getId, dto -> dto));
  }

  public static String getInstrumentName(Integer instrumentId, Map<Integer, InstrumentDto> instrumentsById) {
    InstrumentDto instrument = instrumentsById.get(instrumentId);
    return instrument == null ? "Unknown" : instrument.getName();
  }

  public static String getInstrumentModel(Integer instrumentId, Map<Integer, InstrumentDto> instruments,
      Map<Integer, InstrumentModelDto> models) {
    return models.get(
        instruments.get(instrumentId).getModelId())
        .getName();
  }

}
