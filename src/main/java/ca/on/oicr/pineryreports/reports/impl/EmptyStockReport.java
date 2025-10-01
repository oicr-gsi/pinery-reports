package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.UserDto;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.function.Predicate;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class EmptyStockReport extends TableReport {

    public static final String REPORT_NAME = "empty-stock";
    public static final String CATEGORY = REPORT_CATEGORY_INVENTORY;

    private static final Option OPT_AFTER = CommonOptions.after(false);
    private static final Option OPT_BEFORE = CommonOptions.before(false);
    private static final Option OPT_USER_IDS = CommonOptions.users(true);

    private static final List<ColumnDefinition> COLUMNS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new ColumnDefinition("Sample ID"),
                            new ColumnDefinition("Alias"),
                            new ColumnDefinition("Stock Created"),
                            new ColumnDefinition("Stock Modified"),
                            new ColumnDefinition("Modified By"),
                            new ColumnDefinition("External Name"),
                            new ColumnDefinition("Conc. (ng/µl)"),
                            new ColumnDefinition("Initial Vol. (µl)"),
                            new ColumnDefinition("Current Vol. (µl)"),
                            new ColumnDefinition("Location")));

    private String start;
    private String end;

    private List<SampleDto> stocks;
    private Map<String, SampleDto> allSamplesById;
    private Map<Integer, UserDto> allUsersById;
    private List<Integer> userIds = new ArrayList<>();

    @Override
    public String getReportName() {
        return REPORT_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Collection<Option> getOptions() {
        return Sets.newHashSet(OPT_USER_IDS, OPT_AFTER, OPT_BEFORE);
    }

    @Override
    public void processOptions(CommandLine cmd) throws ParseException {

        String[] users = cmd.getOptionValue(OPT_USER_IDS.getLongOpt()).split(",");
        for (String user : users) {
            if (user != null && !"".equals(user)) {
                userIds.add(Integer.valueOf(user));
            }
        }

        if (cmd.hasOption(OPT_AFTER.getLongOpt())) {
            String after = cmd.getOptionValue(OPT_AFTER.getLongOpt());
            if (!after.matches(DATE_REGEX)) {
                throw new ParseException("After date must be in format yyyy-mm-dd");
            }
            this.start = after;
        }

        if (cmd.hasOption(OPT_BEFORE.getLongOpt())) {
            String before = cmd.getOptionValue(OPT_BEFORE.getLongOpt());
            if (!before.matches(DATE_REGEX)) {
                throw new ParseException("Before date must be in format yyyy-mm-dd");
            }
            this.end = before;
        }
    }

    @Override
    public String getTitle() {
        return "Empty Stock Report for "
                + (start == null ? "Any Time" : start)
                + " - "
                + (end == null ? "Now" : end);
    }

    @Override
    protected void collectData(PineryClient pinery) throws HttpResponseException {
        List<SampleDto> allSamples = pinery.getSample().all();
        List<UserDto> allUsers = pinery.getUser().all();
        allUsersById = mapUsersById(allUsers);
        allSamplesById = mapSamplesById(allSamples);
        stocks = filterReportableStocks(allSamples);
        stocks.sort(byReceiveDateAndName);
    }

    private List<SampleDto> filterReportableStocks(List<SampleDto> unfiltered) {
        Set<Predicate<SampleDto>> filters = Sets.newHashSet();
        filters.add(bySampleCategory(SAMPLE_CATEGORY_STOCK));
        filters.add(byCreator(userIds));
        filters.add(hasDescendants(allSamplesById));
        filters.add(byCreatedBetween(start, end));
        filters.add(sample -> sample.getVolume() != null && sample.getVolume() <= 0);
        return filter(unfiltered, filters);
    }

    /** Sort created date, then name */
    private final Comparator<SampleDto> byReceiveDateAndName =
            (dto1, dto2) -> {
                String dto1Created = removeTime(dto1.getCreatedDate());
                String dto2Created = removeTime(dto2.getCreatedDate());
                int byDate = 0;
                // Should never be null
                if (dto1Created == null) {
                    if (dto2Created != null) {
                        byDate = 1;
                    }
                } else if (dto2Created == null) {
                    byDate = -1;
                } else {
                    byDate = dto1Created.compareTo(dto2Created);
                }
                return byDate == 0 ? dto1.getName().compareTo(dto2.getName()) : byDate;
            };

    @Override
    protected List<ColumnDefinition> getColumns() {
        return COLUMNS;
    }

    @Override
    protected int getRowCount() {
        return stocks.size();
    }

    @Override
    protected String[] getRow(int rowNum) {
        SampleDto stock = stocks.get(rowNum);
        String[] row = new String[COLUMNS.size()];

        row[0] = stock.getId();
        row[1] = stock.getName();
        row[2] = removeTime(stock.getCreatedDate());
        row[3] = removeTime(stock.getModifiedDate());
        UserDto user = allUsersById.get(stock.getModifiedById());
        row[4] = String.format("%s %s", user.getFirstname(), user.getLastname());
        row[5] = getUpstreamAttribute(ATTR_EXTERNAL_NAME, stock, allSamplesById);
        Float concentration = stock.getConcentration();
        row[6] = concentration == null ? null : round(concentration, 2);
        Float initVolume = getFloatAttribute(ATTR_INITIAL_VOLUME, stock);
        row[7] = initVolume == null ? null : round(initVolume, 2);
        row[8] = round(stock.getVolume(), 2);
        row[9] = stock.getStorageLocation();

        return row;
    }

    private static String toStringOrNull(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
