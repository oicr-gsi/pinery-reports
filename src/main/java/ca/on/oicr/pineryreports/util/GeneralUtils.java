package ca.on.oicr.pineryreports.util;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Predicate;

import ca.on.oicr.ws.dto.RunDto;

public class GeneralUtils {

  public static String timeStringToYyyyMmDd(String dateTime) {
    TemporalAccessor temp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateTime);
    return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(temp);
  }
  
  public static Predicate<RunDto> byEndedBetween(String start, String end) {
    return dto -> (start == null || 
        (dto.getCompletionDate() != null && dto.getCompletionDate().compareTo(start) > 0))
      && (end == null || 
        (dto.getCompletionDate() != null && dto.getCompletionDate().compareTo(end) < 0));
  }
}
