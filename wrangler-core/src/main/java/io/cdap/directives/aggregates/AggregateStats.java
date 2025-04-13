
package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.PublicEvolving;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration; // Added import
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.annotations.Usage;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.api.parser.Required; // Changed from Usage.required
import io.cdap.wrangler.api.Store; // Added import

import java.util.Collections;
import java.util.List;

@PublicEvolving
public class AggregateStats implements Directive {
    private String sizeColumn;
    private String timeColumn;
    private String sizeOutputColumn;
    private String timeOutputColumn;
    private String sizeUnit = "MB";
    private String timeUnit = "s";

    @Override
    public UsageDefinition define() {
        return Usage.builder()
            .with("aggregate-stats",
                  Required.of("sizeColumn", TokenType.COLUMN_NAME),
                  Required.of("timeColumn", TokenType.COLUMN_NAME),
                  Required.of("sizeOutputColumn", TokenType.TEXT),
                  Required.of("timeOutputColumn", TokenType.TEXT),
                  Usage.optional("sizeUnit", TokenType.TEXT, new Text(sizeUnit)),
                  Usage.optional("timeUnit", TokenType.TEXT, new Text(timeUnit)))
            .build();
    }

    @Override
    public void initialize(Arguments args) {
        sizeColumn = ((ColumnName) args.value("sizeColumn")).value();
        timeColumn = ((ColumnName) args.value("timeColumn")).value();
        sizeOutputColumn = ((Text) args.value("sizeOutputColumn")).value();
        timeOutputColumn = ((Text) args.value("timeOutputColumn")).value();

        if (args.contains("sizeUnit")) {
            sizeUnit = ((Text) args.value("sizeUnit")).value().toUpperCase();
        }
        if (args.contains("timeUnit")) {
            timeUnit = ((Text) args.value("timeUnit")).value().toLowerCase();
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) {
        Store store = context.getStore();

        for (Row row : rows) {
            String sizeValue = row.getValue(sizeColumn);
            ByteSize byteSize = new ByteSize(sizeValue);
            store.increment("totalBytes", byteSize.getBytes());

            String timeValue = row.getValue(timeColumn);
            TimeDuration timeDuration = new TimeDuration(timeValue);
            store.increment("totalNanos", timeDuration.getNanos());
        }

        if (context.isFinalize()) {
            long totalBytes = store.get("totalBytes");
            long totalNanos = store.get("totalNanos");

            double outputSize;
            switch (sizeUnit) {
                case "KB":
                    outputSize = totalBytes / 1024.0;
                    break;
                case "MB":
                    outputSize = totalBytes / (1024.0 * 1024.0);
                    break;
                case "GB":
                    outputSize = totalBytes / (1024.0 * 1024.0 * 1024.0);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported size unit: " + sizeUnit);
            }

            double outputTime;
            switch (timeUnit) {
                case "ms":
                    outputTime = totalNanos / 1_000_000.0;
                    break;
                case "s":
                    outputTime = totalNanos / 1_000_000_000.0;
                    break;
                case "m":
                    outputTime = totalNanos / (1_000_000_000.0 * 60);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported time unit: " + timeUnit);
            }

            Row result = new Row();
            result.add(sizeOutputColumn, outputSize);
            result.add(timeOutputColumn, outputTime);
            return Collections.singletonList(result);
        }
        return Collections.emptyList();
    }

    @Override
    public void destroy() {
        // Cleanup resources if needed
    }
}
