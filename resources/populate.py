import mysql.connector

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
insert_name("testing")

insert_playlist("testing",["a","b","c"])
