# auth_wrap_test.rb

require 'minitest/autorun'
require './auth_wrap'

class AuthWrapTest < Minitest::Test
  def setup
    @app = GDriveApp.new('Messtin setup app (Ruby)')
  end

  def test_auth
    assert @app.drive
  end

  def test_create_folder
  	folder_id = @app.createFolder('should be in messtin', 'desc', '0B0v3qwjLutgMUTV4UlBvRmd4QnM')
  	@app.uploadJpeg('001.jpg', 'yay', folder_id)
  end
end
