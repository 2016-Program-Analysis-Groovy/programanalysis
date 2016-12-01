/* For detection of sign, can the analysis detect that x is 0 after the while loop? */
/* Courtesy of Frederik Højholt Andersen */
{
    int x; int z;
    x = 10;

    while (x>0) {
        z = z+x;
        x = x-1;
    }
    z = z/x;

}
