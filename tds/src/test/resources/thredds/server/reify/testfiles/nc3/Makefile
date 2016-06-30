NC3=\
test_atomic_types \
test_atomic_array \
test_fill \
test_misc1 \
test_one_var \
test_one_vararray \
test_utf8

KFLAG=-3

all::
	for x in ${NC3} ; do \
	    echo ncgen ${KFLAG} $${x}.cdl ; \
	    ncgen ${KFLAG} $${x}.cdl ; \
	done

clean::
	rm -f *.nc; rm -f *.hdf5; rm -f *.h5
	for x in ${C} ; do rm -f $$x.exe ; done
	rm -fr dump

verify:
	rm -fr dump
	mkdir dump
	for x in ${NC3} ; do ncdump $${x}.nc > dump/$${x}.cdl ; done
	for x in ${NC3} ; do \
	echo $${x}.cdl ; \
	diff -wBb $${x}.cdl dump/$${x}.cdl ; \
	done

