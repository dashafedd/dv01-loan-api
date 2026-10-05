package loan.api.loader

import dv01.loan.api.model.Loan
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class CsvLoanLoader {

    fun load(path: Path): List<Loan> {
        require(Files.isReadable(path)) {
            "Loan data file is not found or not readable: ${path.toAbsolutePath()}."
        }

        return emptyList();
    }
}