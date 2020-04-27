import multiprocessing
import server2
import buildAndroid2
multiprocessing.Process(target=server2.mainx).start()
multiprocessing.Process(target=buildAndroid2.mainx).start()