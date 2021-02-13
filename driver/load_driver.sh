if [ `lsmod | grep -o riscv_driver` ]; then
	echo "RISCV-Driver driver has already been loaded. Doing nothing."
	exit
fi
insmod riscv-driver.ko

#Find what major device number was assigned from /proc/devices
majorNum=$( awk '{ if ($2 ~ /vcu1525/) print $1}' /proc/devices )

if [ -z "$majorNum" ]; then
	echo "Unable to find the VCU1525 device!"
	echo "Did the driver correctly load?"
else
	#Remove any stale device file
	if [ -e "/dev/vcu1525" ]; then
		rm -r /dev/vcu1525
	fi

	#Create a new one with full read/write permissions for everyone
	sudo mknod -m 666 /dev/vcu1525 c $majorNum 0
fi

