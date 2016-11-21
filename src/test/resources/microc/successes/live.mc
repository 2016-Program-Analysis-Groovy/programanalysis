/* For live variable analysis. Is there a label where all three variables are live? */
{
    int x; int y; int z;
    
    read x;
    y = x + 1;

    if (y == 16) {
        y = 4;
    }
    read z;
    write z + y;

    x = x*x;

    read z;
    write 2 * z + y;

    x = 12 + y;
    write x;
}
