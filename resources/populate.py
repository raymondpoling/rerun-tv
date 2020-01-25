import mysql.connector
from os import listdir
from os.path import isfile, join, isdir

series_prefix = "series_"

mydb = mysql.connector.connect(
  host="localhost",
  user="playlist_user",
  passwd="playlist"
)

cursor = mydb.cursor()

add_playlist_name = ("INSERT INTO playlist.name (name) VALUES (%s)")

select_playlist_name = ("SELECT (id) FROM playlist.name WHERE name = %s")

add_playlist_elements = ("INSERT INTO playlist.playlist "
                        "(name_key,idx,object) "
                        "VALUES (%s,%s,%s)")

def insert_name(name):
    cursor.execute(add_playlist_name,(series_prefix + name,))
    mydb.commit()

def insert_playlist(name,list_of_items):
    prepare = zip(range(len(list_of_items)),list_of_items)
    cursor.execute(select_playlist_name,(series_prefix + name,))
    n = cursor.fetchone()[0]
    print "this is n: " + str(n)
    for (idx,obj) in prepare:
        cursor.execute(add_playlist_elements,(n,idx,obj))
    mydb.commit()

# Quick check below
#insert_name("testing")

#insert_playlist("testing",["a","b","c"])

def walking(rootdir):
    listing = listdir(rootdir)
    listing.remove("movies")
    subdirs = [f for f in listing if isdir(join(rootdir, f))]
    for series in subdirs:
        print "Inserting name: " + series + "\n"
        insert_name(series)
        episodes = []
        all_seasons = [f for f in listdir(join(rootdir,series)) if isdir(join(rootdir, series, f))]
        for season in all_seasons:
            episodes = episodes + [join(rootdir,series,season,f) for f in listdir(join(rootdir,series,season)) if isfile(join(rootdir,series,season,f))]
        episodes.sort()
        insert_playlist(series,episodes)

walking("/home/ruguer/Videos/")
