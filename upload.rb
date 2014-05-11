require './auth_wrap'

title = ARGV.shift
dir = ARGV.shift

app = GDriveApp.new('messtin')

mid = app.messtinFolder
puts mid

folder_id = app.createFolder(title, title, mid)
tm_id = app.createFolder('tm', 'thumbnail', folder_id)

Dir.glob(dir + '/*.jpg').each do |jpg|
  name = File.basename(jpg)
  puts name
  app.uploadJpeg(jpg, name, folder_id)
end

Dir.glob(dir + '/tm/*.jpg').each do |jpg|
  name = File.basename(jpg)
  puts name
  app.uploadJpeg(jpg, name, tm_id)
end