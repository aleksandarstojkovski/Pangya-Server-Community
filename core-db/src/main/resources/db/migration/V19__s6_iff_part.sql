-- SQL stand-in for C# IFF Part.valor_rental (requestExtendRental / requestDeleteRental).
-- valor_rental <= 0 is not a rental. Catalog rows are test-inserted.
CREATE TABLE pangya.iff_part (
    typeid INTEGER PRIMARY KEY,
    valor_rental BIGINT NOT NULL
);
