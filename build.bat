rm test.obj
rm test.exe
nasm -f win32 test.asm
gcc test.obj -o test.exe -lkernel32

echo "


"
./test.exe
result=$?

echo "

Program returned "$result

read -n1 -r -p "Press any key to continue..." key