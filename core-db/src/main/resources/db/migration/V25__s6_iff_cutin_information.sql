-- SQL stand-in for C# CutinInfomation.iff fields serialized by requestActiveCutin.
CREATE TABLE pangya.iff_cutin_information (
    typeid INTEGER PRIMARY KEY,
    sector INTEGER NOT NULL,
    condition INTEGER NOT NULL,
    img0_tipo INTEGER NOT NULL,
    img1_tipo INTEGER NOT NULL,
    img2_tipo INTEGER NOT NULL,
    img3_tipo INTEGER NOT NULL,
    tempo INTEGER NOT NULL,
    sprite0 VARCHAR(40) NOT NULL,
    sprite1 VARCHAR(40) NOT NULL,
    sprite2 VARCHAR(40) NOT NULL,
    sprite3 VARCHAR(40) NOT NULL
);
